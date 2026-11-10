#!/usr/bin/env python3
"""Summarize timestamped k6 JSON metrics in one-second CSV buckets."""

import argparse
import csv
import gzip
import json
import math
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path

COUNTERS = ("http_reqs", "dropped_iterations")
DURATIONS = (
    "http_req_duration",
    "http_req_waiting",
    "http_req_connecting",
    "http_req_blocked",
    "iteration_duration",
)
METRICS = {*COUNTERS, *DURATIONS, "vus"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("metrics", type=Path, help="Path to k6-metrics.json.gz")
    parser.add_argument("--output", required=True, type=Path, help="New CSV file")
    parser.add_argument(
        "--start", type=datetime.fromisoformat,
        help="ISO 8601 scenario start with timezone. Defaults to adjacent start-time.txt.",
    )
    args = parser.parse_args()
    start = args.start
    if start is None:
        try:
            start = datetime.fromisoformat(
                args.metrics.with_name("start-time.txt").read_text().strip()
            )
        except (OSError, ValueError) as exc:
            parser.error(f"Provide --start or a readable start-time.txt: {exc}")
    if start.utcoffset() is None:
        parser.error("The start time must include a timezone")

    buckets = defaultdict(lambda: defaultdict(list))
    with gzip.open(args.metrics, "rt", encoding="utf-8") as source:
        for line in source:
            point = json.loads(line)
            if point["type"] != "Point" or point["metric"] not in METRICS:
                continue
            data = point["data"]
            observed = datetime.fromisoformat(data["time"])
            second = math.floor((observed - start).total_seconds())
            buckets[second][point["metric"]].append(data["value"])

    if not buckets:
        parser.error("No supported metric points found")

    columns = ["elapsed_second", "bucket_start", *COUNTERS]
    for metric in DURATIONS:
        columns.extend((f"{metric}_p95_ms", f"{metric}_max_ms"))
    columns.append("vus_sample_max")

    with args.output.open("x", newline="", encoding="utf-8") as destination:
        writer = csv.writer(destination)
        writer.writerow(columns)
        for second in range(min(0, min(buckets)), max(buckets) + 1):
            metrics = buckets.get(second, {})
            row = [
                second,
                (start + timedelta(seconds=second)).isoformat(),
                *(sum(metrics.get(metric, ())) for metric in COUNTERS),
            ]
            for metric in DURATIONS:
                values = sorted(metrics.get(metric, ()))
                if values:
                    # Nearest-rank percentile within this one-second bucket.
                    row.extend((values[math.ceil(0.95 * len(values)) - 1], values[-1]))
                else:
                    row.extend((None, None))
            row.append(max(metrics.get("vus", ()), default=None))
            writer.writerow(row)


if __name__ == "__main__":
    main()
