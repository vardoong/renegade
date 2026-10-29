package com.renegade.vehicle;

import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> create(
            @Valid @RequestBody VehicleCreateRequest vehicleCreateRequest
    ) {
        VehicleResponse response = vehicleService.create(vehicleCreateRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> findById(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(vehicleService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<VehicleResponse>> getList(
            @PageableDefault(
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(vehicleService.getList(pageable));
    }
}
