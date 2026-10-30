package com.renegade.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Renegade API",
                version = "v1",
                description = """
                        Connected Vehicle Observability Backend

                        차량 정보와 텔레메트리 데이터를 수집·저장·조회하는 REST API입니다.

                        주요 기능:
                        - 차량 등록 및 조회
                        - 텔레메트리 데이터 수집
                        - 차량별 텔레메트리 이력 조회
                        - 최신 텔레메트리 조회
                        """
        )
)
public class OpenApiConfig {
}