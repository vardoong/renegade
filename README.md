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

---

## Getting Started

### Prerequisites

- Java 21
- Docker / Docker Compose

### Environment Variables

프로젝트 루트에 있는 `.env.example` 파일을 복사해서 `.env` 파일 생성

```shell
cp .env.example .env
```

생성된 `.env` 파일의 값을 로컬 환경에 맞게 수정

- `.env.example` 환경변수 설정 예시 파일 (git 포함)
- `.env` 실제 환경변수 설정 파일 (git 제외)

### Run

1. PostgreSQL 실행
```shell
docker compose up -d
```
2. 환경변수 적용
```shell
set -a
source .env
set +a
```
3. 테스트 실행
```shell
./gradlew test
```
4. 애플리케이션 실행
```shell
./gradlew bootRun
```

### API Documentation

애플리케이션 실행 후 Swagger UI에서 API 명세를 확인하고 직접 요청을 테스트할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Tests

PostgreSQL 실행 및 환경변수 적용 후 다음 명령으로 전체 테스트를 실행합니다.

```shell
./gradlew clean test
```

- JUnit 5 기반 단위 테스트
- Controller API 테스트
- Testcontainers 기반 Repository 통합 테스트
- 입력값 유효성 및 예외 처리 테스트

---

## Phase 1

- Vehicle registration
- Vehicle telemetry collection
- Vehicle current status query
- Vehicle telemetry history query
- Vehicle simulator

## Status

🚧 Phase 1 - In Development