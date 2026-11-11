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
3. 애플리케이션 실행
```shell
./gradlew bootRun
```

### API Documentation

애플리케이션 실행 후 Swagger UI에서 API 명세를 확인하고 직접 요청을 테스트할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Tests

- Java 21
- Docker
- 개발용 PostgreSQL이나 `.env` 설정 없이 테스트
  - Testcontainers가 임시 PostgreSQL을 준비
  - Flyway가 스키마를 적용
  - 테스트 종료 후 임시 컨테이너 정리

테스트 실행:

```shell
./gradlew clean test
```

- JUnit 5 기반 단위 테스트
- Controller API 테스트
- Testcontainers 기반 Repository 통합 테스트
- Testcontainers 기반 애플리케이션 기동 테스트 (`contextLoads`)
- 입력값 유효성 및 예외 처리 테스트

개발 DB 환경변수 없이 실행:

```shell
env -u POSTGRES_DB -u POSTGRES_USER -u POSTGRES_PASSWORD \
  ./gradlew clean test
```

- 테스트 결과 보고서: `build/reports/tests/test/index.html`

참고사항:
- Simulator의 Python 테스트는 별도로 실행
- 준비 및 실행 방법은 [Simulator README](simulator/README.md#테스트) 참고

### CI

- GitHub Actions가 push·PR마다 `./gradlew clean test`로 전체 백엔드 테스트 실행
  - 저장소의 **Actions → Backend Tests**에서 결과 확인
  - `test-reports` 아티팩트에서 테스트 보고서 확인

---

## Troubleshooting — 커넥션 풀 획득 지연 개선

1. 로컬 2,400 req/s 쓰기 부하에서 순간 지연과 반복 누락 발생
2. 요청별 계측으로 커넥션 획득 지연 확인, 풀 10개 사용·61개 스레드 대기 관측
3. 동일 DB 백업에서 풀 10·15를 각각 4분·150 VU로 재검증
   1. 풀 15개에서 획득·응답 지연이 줄고 반복 누락이 관측되지 않아 최종 채택

| 지표 | 풀 10 | 풀 15 |
| --- | ---: | ---: |
| k6 전체 HTTP 응답 p99 | 8.74 ms | 6.67 ms |
| k6 전체 HTTP 응답 최대 | 168.58 ms | 108.74 ms |
| 앱 커넥션 획득 시간 최대 | 165.30 ms | 96.02 ms |
| 시작하지 못한 반복 (`dropped_iterations`) | 87건 | 0건 |

두 실행 모두 HTTP·체크·저장 실패는 0건이며, 성공 건수와 DB 행 증가량이 일치했습니다.
조건당 1회 비교한 결과로, 현재 로컬 부하에 한정한 선택입니다. 포화의 촉발 원인은 미확정입니다.

상세 조건과 조사 과정, 원본 결과는 [Day 9 측정 기록](docs/measurements/2026-11-10-k6-2400-vu150.md)을 참고하세요.

---

## Phase 1

- Vehicle registration
- Vehicle telemetry collection
- Vehicle current status query
- Vehicle telemetry history query
- Vehicle simulator

## Status

🚧 Phase 1 - In Development
