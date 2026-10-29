package com.renegade.vehicle;

import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse create(VehicleCreateRequest req) {

        if(vehicleRepository.existsByExternalId(req.externalId())) {
            throw new IllegalArgumentException("이미 등록된 차량입니다.");
        }

        Vehicle vehicle = Vehicle.builder()
                .externalId(req.externalId())
                .modelName(req.modelName())
                .name(req.name())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.from(savedVehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id){
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getList(Pageable pageable) {
        return vehicleRepository.findAll(pageable)
                .map(VehicleResponse::from);
    }
}
