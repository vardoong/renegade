# Renegade

Backend system for collecting, storing, and querying vehicle telemetry data.

## Overview

Renegade는 차량에서 발생하는 데이터를 수집·저장하고,
차량의 현재 상태와 데이터 이력을 조회할 수 있는 백엔드 시스템입니다.

Phase 1에서는 실제 차량 대신 Simulator를 통해 차량 데이터를 생성합니다.

## Architecture

Vehicle Simulator → Renegade Backend → PostgreSQL

## Tech Stack

- Java 21
- Spring Boot 4.1
- Gradle
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker / Docker Compose
- Spring Boot Actuator
- JUnit 5
- Testcontainers
- Swagger / OpenAPI
- Python (Vehicle Simulator)

## Phase 1

- Vehicle registration
- Vehicle telemetry collection
- Vehicle current status query
- Vehicle telemetry history query
- Vehicle simulator

## Status

🚧 Phase 1 - In Development