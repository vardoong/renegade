#!/usr/bin/env python3
"""Run telemetry k6 and record Docker/macOS process resource samples."""

import argparse
import csv
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import threading
import time
from zoneinfo import ZoneInfo


def timestamp():
    return datetime.now(ZoneInfo("Asia/Seoul")).isoformat(timespec="seconds")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path,
                        help="New result directory (must not already exist)")
    parser.add_argument("--rate", type=int, default=1200)
    parser.add_argument("--duration", default="4m")
    parser.add_argument("--interval", type=float, default=5)
    parser.add_argument("--container", default="renegade-postgres")
    args = parser.parse_args()
    if args.rate <= 0 or args.interval <= 0:
        parser.error("rate and interval must be positive")
    if sys.platform != "darwin":
        parser.error("k6 process sampling is intended for macOS ps")
    for command in ("k6", "docker", "ps"):
        if not shutil.which(command):
            parser.error(f"Required command not found: {command}")
    try:
        state = subprocess.run(
            ["docker", "inspect", "--format", "{{.State.Running}}", args.container],
            capture_output=True, text=True, check=True, timeout=10,
        )
        if state.stdout.strip() != "true":
            parser.error("PostgreSQL container is not running")
    except (subprocess.SubprocessError, OSError) as exc:
        parser.error(f"Cannot inspect PostgreSQL container: {exc}")

    # Refuse reuse so repeated measurements never overwrite each other.
    args.output.mkdir(parents=True, exist_ok=False)
    output = args.output.resolve()
    script = Path(__file__).resolve().with_name("telemetry.js")
    command = [
        "k6", "run",
        f"--summary-export={output / 'summary.json'}",
        "--out", f"json={output / 'k6-metrics.json.gz'}",
        str(script),
    ]
    env = dict(os.environ, RATE=str(args.rate), DURATION=args.duration)
    metadata = {
        "start_time": timestamp(), "command": command,
        "rate": args.rate, "duration": args.duration,
        "interval_seconds": args.interval, "container": args.container,
        "base_url": env.get("BASE_URL", "http://localhost:8080"),
        "units": {
            "db_cpu_percent": "Docker stats CPU percentage; may exceed 100% across cores",
            "db_memory_usage": "Docker stats MemUsage, including displayed units",
            "k6_cpu_percent": "macOS ps %cpu (process CPU average, not interval delta); may exceed 100%",
            "k6_rss_kib": "macOS ps RSS in KiB",
        },
    }
    stop = threading.Event()
    errors = []

    def collect(filename, fields, sample):
        with (output / filename).open("w", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerow(["timestamp_kst", *fields, "error"])
            while not stop.is_set():
                started = time.monotonic()
                observed = timestamp()
                try:
                    writer.writerow([observed, *sample(), ""])
                except (subprocess.SubprocessError, OSError, ValueError, KeyError) as exc:
                    detail = str(exc)
                    errors.append(f"{filename}: {detail}")
                    writer.writerow([observed, *([""] * len(fields)), detail])
                handle.flush()
                stop.wait(max(0, args.interval - (time.monotonic() - started)))

    def db_sample():
        result = subprocess.run(
            ["docker", "stats", "--no-stream", "--format", "{{json .}}", args.container],
            capture_output=True, text=True, check=True, timeout=10,
        )
        stats = json.loads(result.stdout)
        return [stats["CPUPerc"].rstrip("%"), stats["MemUsage"],
                stats["MemPerc"].rstrip("%")]

    (output / "start-time.txt").write_text(metadata["start_time"] + "\n")
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    with (output / "console.log").open("w") as log:
        try:
            process = subprocess.Popen(command, env=env, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, text=True)
        except OSError as exc:
            log.write(str(exc) + "\n")
            (output / "exit-code.txt").write_text("127\n")
            (output / "end-time.txt").write_text(timestamp() + "\n")
            return 127

        def copy_console():
            for line in process.stdout:
                log.write(line)
                log.flush()
                sys.stdout.write(line)
                sys.stdout.flush()

        def k6_sample():
            result = subprocess.run(
                ["ps", "-p", str(process.pid), "-o", "%cpu=,rss="],
                capture_output=True, text=True, timeout=5,
            )
            values = result.stdout.split()
            if len(values) != 2:
                raise ValueError("k6 process exited or ps could not read it")
            return [process.pid, *values]

        console = threading.Thread(target=copy_console)
        collectors = [
            threading.Thread(target=collect, args=(
                "db-resources.csv", ["cpu_percent", "memory_usage", "memory_percent"], db_sample)),
            threading.Thread(target=collect, args=(
                "k6-resources.csv", ["pid", "cpu_percent", "rss_kib"], k6_sample)),
        ]
        console.start()
        for worker in collectors:
            worker.start()
        try:
            code = process.wait()
        except KeyboardInterrupt:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
            code = 130
        finally:
            metadata["end_time"] = timestamp()
            stop.set()
            for worker in collectors:
                worker.join()
            console.join()
            process.stdout.close()

    metadata.update(k6_pid=process.pid, exit_code=code, sampling_errors=errors)
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    (output / "end-time.txt").write_text(metadata["end_time"] + "\n")
    (output / "exit-code.txt").write_text(str(code) + "\n")
    print(f"Results: {output}")
    if errors:
        print("Resource sampling errors occurred; inspect CSV error columns.", file=sys.stderr)
    return code if code else (1 if errors else 0)


if __name__ == "__main__":
    sys.exit(main())
