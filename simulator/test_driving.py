from vehicle import VehicleState
from driving import DrivingSimulator


vehicle = VehicleState(vehicle_id=1)
simulator = DrivingSimulator(vehicle)

for second in range(60):
    state = simulator.update()

    print(
        f"[{second + 1:02d}s] "
        f"Mode={simulator.mode.value:12} "
        f"Speed={state.speed:5.1f} km/h "
        f"RPM={state.rpm:4d} "
        f"Battery={state.battery_level:3d}% "
        f"Temp={state.temperature:.2f}°C "
        f"GPS=({state.latitude:.6f}, {state.longitude:.6f})"
    )