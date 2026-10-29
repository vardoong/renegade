package com.renegade.telemetry;

import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
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
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/api/v1/telemetry")
    public ResponseEntity<TelemetryResponse> create(
            @Valid @RequestBody TelemetryCreateRequest telemetryCreateRequest
    ) {
        TelemetryResponse response = telemetryService.create(telemetryCreateRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/v1/telemetry/{id}")
    public ResponseEntity<TelemetryResponse> findById(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(telemetryService.get(id));
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/telemetry")
    public ResponseEntity<Page<TelemetryResponse>> findList(
            @PathVariable("vehicleId") Long vehicleId,
            @PageableDefault(
                    size = 20,
                    sort = {"timestamp", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(telemetryService.getList(vehicleId, pageable));
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/telemetry/latest")
    public ResponseEntity<TelemetryResponse> findLatest(
            @PathVariable("vehicleId") Long vehicleId
    ) {
        return ResponseEntity.ok(telemetryService.getLatest(vehicleId));
    }
}
