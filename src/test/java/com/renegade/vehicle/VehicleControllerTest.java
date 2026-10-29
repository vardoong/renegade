package com.renegade.vehicle;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    void 차량_단건_조회_성공() throws Exception {
        // given
        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.get(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.modelName").value("IONIQ 5"));

        verify(vehicleService).get(1L);
    }

    @Test
    void 차량_목록_조회_성공() throws Exception {
        // given
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "id")
        );

        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.getList(pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        // when & then
        mockMvc.perform(get("/api/v1/vehicles")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(vehicleService).getList(pageable);
    }

    @Test
    void 차량_등록_성공() throws Exception {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.create(any(VehicleCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.modelName").value("IONIQ 5"))
                .andExpect(jsonPath("$.name").value("Test Vehicle"));

        verify(vehicleService).create(any(VehicleCreateRequest.class));
    }

    @Test
    void 차량_등록_필수값_누락_실패() throws Exception {
        // given
        String invalidRequest = """
            {
                "externalId": "",
                "modelName": "IONIQ 5",
                "name": "Test Vehicle"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never())
                .create(any(VehicleCreateRequest.class));
    }

    @Test
    void 존재하지_않는_차량_조회시_404_반환() throws Exception {
        // given
        Long vehicleId = 999L;

        when(vehicleService.get(vehicleId))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isNotFound());

        verify(vehicleService).get(vehicleId);
    }

    @Test
    void 중복_차량_등록시_409_반환() throws Exception {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        when(vehicleService.create(any(VehicleCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(vehicleService).create(any(VehicleCreateRequest.class));
    }
}