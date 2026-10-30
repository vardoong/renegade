package com.renegade.vehicle;

import com.renegade.global.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@EnableJpaAuditing
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class VehicleRepositoryIntegrationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    @DisplayName("차량을 저장하고 조회할 수 있다")
    void shouldSaveAndFindVehicle() {
        // given
        Vehicle vehicle = Vehicle.builder()
                .externalId("VEHICLE-001")
                .modelName("IONIQ 5")
                .name("Test Vehicle")
                .build();

        // when
        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        Vehicle found = vehicleRepository.findById(saved.getId())
                .orElseThrow();

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getExternalId()).isEqualTo("VEHICLE-001");
        assertThat(found.getModelName()).isEqualTo("IONIQ 5");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일한 externalId로 차량을 저장하면 예외가 발생한다")
    void shouldRejectDuplicateExternalId()  {
        // given
        Vehicle first = Vehicle.builder()
                .externalId("VEHICLE-001")
                .modelName("IONIQ 5")
                .name("First Vehicle")
                .build();

        Vehicle duplicate = Vehicle.builder()
                .externalId("VEHICLE-001")
                .modelName("IONIQ 6")
                .name("Second Vehicle")
                .build();

        vehicleRepository.saveAndFlush(first);

        // when & then
        assertThatThrownBy(() -> vehicleRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("차량 목록을 ID 내림차순으로 페이징 조회한다")
    void shouldFindVehiclesPagedAndSortedByIdDescending() {
        // given
        for (int i = 1; i <= 5; i++) {
            vehicleRepository.save(Vehicle.builder()
                    .externalId("VEHICLE-" + i)
                    .modelName("IONIQ 5")
                    .name("Vehicle " + i)
                    .build());
        }
        vehicleRepository.flush();

        PageRequest pageable = PageRequest.of(
                1, 2, Sort.by(Sort.Direction.DESC, "id")
        );

        // when
        Page<Vehicle> page = vehicleRepository.findAll(pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(Vehicle::getExternalId)
                .containsExactly("VEHICLE-3", "VEHICLE-2");
    }
}
