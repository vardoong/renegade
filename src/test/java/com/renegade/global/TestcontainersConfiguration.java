package com.renegade.global;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /*
    * @ServiceConnection
    * spring boot 가 testcontainers 의 postgreSQL 연결 정보를 자동으로 사용.
    * 테스트용 db 주소나 포트 지정 안해도 됨.
    * */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresqlContainer() {
        return new PostgreSQLContainer("postgres:17");
    }
}
