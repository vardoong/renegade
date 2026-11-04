import os
import time
import logging
import math
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Event
from datetime import datetime, timezone

from dotenv import load_dotenv

from client import TelemetryClient
from vehicle import VehicleState
from driving import DrivingSimulator


def run_vehicle(base_url, vehicle_id, interval, deadline, stop):
    client = TelemetryClient(base_url)

    vehicle = VehicleState(vehicle_id=vehicle_id)
    simulator = DrivingSimulator(vehicle)
    succeeded = failed = 0

    print(f"Vehicle Simulator started (vehicleId={vehicle_id})")

    try:
        next_send = time.monotonic()
        while not stop.is_set() and next_send < deadline:
            if stop.wait(max(0, next_send - time.monotonic())) or time.monotonic() >= deadline:
                break

            state = simulator.update(delta_time=interval)

            telemetry = {
                "vehicleId": state.vehicle_id,
                "speed": round(state.speed, 2),
                "rpm": state.rpm,
                "batteryLevel": state.battery_level,
                "temperature": round(state.temperature, 2),
                "latitude": round(state.latitude, 6),
                "longitude": round(state.longitude, 6),
                "timestamp": datetime.now(timezone.utc).isoformat()
            }

            response = client.send(telemetry)

            if response is not None:
                succeeded += 1
                print(
                    f"[SUCCESS] Vehicle={state.vehicle_id} "
                    f"Mode={simulator.mode.value} "
                    f"Speed={state.speed:.1f} km/h "
                    f"RPM={state.rpm} "
                    f"Telemetry ID={response['id']}"
                )
            else:
                failed += 1
                print(
                    f"[FAILED] "
                    f"Vehicle={state.vehicle_id} "
                    f"Speed={state.speed:.1f} km/h"
                )

            next_send += interval
            now = time.monotonic()
            if next_send < now:
                next_send = now + interval

    finally:
        client.session.close()

    return succeeded, failed


def main():
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S"
    )

    load_dotenv()

    base_url = os.getenv("API_BASE_URL", "http://localhost:8080")
    vehicle_ids = [
        int(value)
        for value in os.getenv("VEHICLE_IDS", os.getenv("VEHICLE_ID", "1")).split(",")
    ]
    interval = float(os.getenv("SEND_INTERVAL_SECONDS", "1"))
    duration = float(os.getenv("RUN_DURATION_SECONDS", "0"))

    if any(vehicle_id <= 0 for vehicle_id in vehicle_ids) or len(set(vehicle_ids)) != len(vehicle_ids):
        raise ValueError("Vehicle IDs must be positive and unique")
    if not math.isfinite(interval) or interval <= 0:
        raise ValueError("SEND_INTERVAL_SECONDS must be finite and greater than 0")
    if not math.isfinite(duration) or duration < 0:
        raise ValueError("RUN_DURATION_SECONDS must be finite and non-negative")

    stop = Event()
    started_at = datetime.now(timezone.utc)
    started = time.monotonic()
    deadline = started + duration if duration else math.inf
    runs = {}
    with ThreadPoolExecutor(max_workers=len(vehicle_ids)) as executor:
        try:
            for vehicle_id in vehicle_ids:
                run = executor.submit(run_vehicle, base_url, vehicle_id, interval, deadline, stop)
                runs[run] = vehicle_id
            for run in as_completed(runs):
                run.result()
        except KeyboardInterrupt:
            print("\nVehicle Simulator stopped")
        finally:
            stop.set()

    ended_at = datetime.now(timezone.utc)
    elapsed = time.monotonic() - started
    print(f"\nRun summary: started={started_at.isoformat()} ended={ended_at.isoformat()} elapsed={elapsed:.2f}s")

    total_succeeded = total_failed = 0
    for run, vehicle_id in runs.items():
        succeeded, failed = run.result()
        total_succeeded += succeeded
        total_failed += failed
        print(f"Vehicle={vehicle_id} attempts={succeeded + failed} succeeded={succeeded} failed={failed}")

    attempts = total_succeeded + total_failed
    print(f"Total attempts={attempts} succeeded={total_succeeded} failed={total_failed}")
    print(f"Telemetry/sec: target={len(vehicle_ids) / interval:.2f} actual={attempts / elapsed:.2f}")


if __name__ == "__main__":
    main()
