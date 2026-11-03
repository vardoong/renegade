import pytest

from vehicle import VehicleState
from driving import DrivingSimulator, DrivingMode


@pytest.fixture
def simulator():
    vehicle = VehicleState(vehicle_id=1)
    return DrivingSimulator(vehicle)


def test_initial_state(simulator):
    """초기 차량 상태 검증"""
    assert simulator.mode == DrivingMode.IDLE
    assert simulator.vehicle.speed == 0
    assert simulator.vehicle.battery_level == 100


def test_idle_to_accelerating(simulator):
    """3초 후 가속 상태로 전환되는지 검증"""
    for _ in range(3):
        simulator.update(1.0)

    assert simulator.mode == DrivingMode.ACCELERATING


def test_acceleration(simulator):
    """가속 상태에서 속도가 증가하는지 검증"""
    simulator.change_mode(DrivingMode.ACCELERATING)

    simulator.update(1.0)

    assert simulator.vehicle.speed == pytest.approx(3.0)
    assert simulator.vehicle.rpm == 120


def test_speed_limit(simulator):
    """최대 속도 60km/h를 초과하지 않는지 검증"""
    simulator.change_mode(DrivingMode.ACCELERATING)

    for _ in range(20):
        simulator.update(1.0)

    assert simulator.vehicle.speed <= 60
    assert simulator.mode == DrivingMode.CRUISING


def test_cruising_to_braking(simulator):
    """정속 주행 후 감속 상태로 전환되는지 검증"""
    simulator.change_mode(DrivingMode.CRUISING)

    for _ in range(10):
        simulator.update(1.0)

    assert simulator.mode == DrivingMode.BRAKING


def test_braking(simulator):
    """감속 상태에서 속도가 감소하는지 검증"""
    simulator.vehicle.speed = 60
    simulator.change_mode(DrivingMode.BRAKING)

    simulator.update(1.0)

    assert simulator.vehicle.speed == pytest.approx(55.0)


def test_battery_range(simulator):
    """배터리 잔량이 0~100% 범위를 유지하는지 검증"""
    simulator.change_mode(DrivingMode.CRUISING)

    for _ in range(100):
        simulator.update(1.0)

    assert 0 <= simulator.vehicle.battery_level <= 100


def test_temperature_range(simulator):
    """온도가 설정된 최대값을 초과하지 않는지 검증"""
    simulator.change_mode(DrivingMode.CRUISING)

    for _ in range(100):
        simulator.update(1.0)

    assert 25 <= simulator.vehicle.temperature <= 45


def test_gps_update(simulator):
    """주행 중 GPS 위치가 변경되는지 검증"""
    initial_latitude = simulator.vehicle.latitude

    simulator.change_mode(DrivingMode.CRUISING)
    simulator.vehicle.speed = 60

    simulator.update(1.0)

    assert simulator.vehicle.latitude > initial_latitude