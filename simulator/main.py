import os
import time
import logging
from datetime import datetime, timezone

from dotenv import load_dotenv

from client import TelemetryClient
from vehicle import VehicleState
from driving import DrivingSimulator


def main():
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S"
    )

    load_dotenv()

    base_url = os.getenv("API_BASE_URL", "http://localhost:8080")
    vehicle_id = int(os.getenv("VEHICLE_ID", "1"))
    interval = float(os.getenv("SEND_INTERVAL_SECONDS", "1"))

    if interval <= 0:
        raise ValueError("SEND_INTERVAL_SECONDS must be greater than 0")

    client = TelemetryClient(base_url)

    vehicle = VehicleState(vehicle_id=vehicle_id)
    simulator = DrivingSimulator(vehicle)

    print(f"Vehicle Simulator started (vehicleId={vehicle_id})")

    try:
        while True:
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
                print(
                    f"[SUCCESS] "
                    f"Mode={simulator.mode.value} "
                    f"Speed={state.speed:.1f} km/h "
                    f"RPM={state.rpm} "
                    f"Telemetry ID={response['id']}"
                )
            else:
                print(
                    f"[FAILED] "
                    f"Vehicle={state.vehicle_id} "
                    f"Speed={state.speed:.1f} km/h"
                )

            time.sleep(interval)

    except KeyboardInterrupt:
        print("\nVehicle Simulator stopped")


if __name__ == "__main__":
    main()