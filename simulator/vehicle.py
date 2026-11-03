from dataclasses import dataclass


@dataclass
class VehicleState:
    vehicle_id: int
    speed: float = 0.0
    rpm: int = 0
    battery_level: int = 100
    temperature: float = 25.0
    latitude: float = 37.5665
    longitude: float = 126.9780

    # 내부 계산용
    battery_remaining: float = 100.0