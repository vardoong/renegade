from enum import Enum
import math


class DrivingMode(Enum):
    IDLE = "idle"
    ACCELERATING = "accelerating"
    CRUISING = "cruising"
    BRAKING = "braking"


class DrivingSimulator:

    def __init__(self, vehicle):
        self.vehicle = vehicle
        self.mode = DrivingMode.IDLE
        self.elapsed = 0

    def update(self, delta_time: float = 1.0):
        self.elapsed += delta_time

        if self.mode == DrivingMode.IDLE:
            self.vehicle.speed = 0
            self.vehicle.rpm = 0

            if self.elapsed >= 3:
                self.change_mode(DrivingMode.ACCELERATING)

        elif self.mode == DrivingMode.ACCELERATING:
            self.vehicle.speed = min(
                60,
                self.vehicle.speed + 3.0 * delta_time
            )

            if self.vehicle.speed >= 60:
                self.change_mode(DrivingMode.CRUISING)

        elif self.mode == DrivingMode.CRUISING:
            self.vehicle.speed = 60

            if self.elapsed >= 10:
                self.change_mode(DrivingMode.BRAKING)

        elif self.mode == DrivingMode.BRAKING:
            self.vehicle.speed = max(
                0,
                self.vehicle.speed - 5.0 * delta_time
            )

            if self.vehicle.speed <= 0:
                self.change_mode(DrivingMode.IDLE)

        # 단순화한 가상 모터 RPM 모델
        self.vehicle.rpm = int(self.vehicle.speed * 40)

        self.update_sensors(delta_time)

        return self.vehicle

    def change_mode(self, mode: DrivingMode):
        self.mode = mode
        self.elapsed = 0

    def update_sensors(self, delta_time: float):
        # 이동 거리 (km)
        distance_km = self.vehicle.speed * delta_time / 3600

        # 배터리 소모: 1km당 0.15% (가상 모델)
        self.vehicle.battery_remaining = max(
            0.0,
            self.vehicle.battery_remaining - distance_km * 0.15
        )
        self.vehicle.battery_level = int(
            self.vehicle.battery_remaining
        )

        # 온도: 주행 중 상승, 정차 중 하강
        if self.vehicle.speed > 0:
            self.vehicle.temperature = min(
                45.0,
                self.vehicle.temperature + 0.02 * delta_time
            )
        else:
            self.vehicle.temperature = max(
                25.0,
                self.vehicle.temperature - 0.01 * delta_time
            )

        # GPS: 북쪽으로 이동하는 단순 모델
        earth_radius_km = 6371.0

        latitude_delta = (
                                 distance_km / earth_radius_km
                         ) * (180 / math.pi)

        self.vehicle.latitude += latitude_delta