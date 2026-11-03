# 모니터링 실행 가이드

## 1. 실행 준비

프로젝트 루트에 `.env` 파일을 준비하고 PostgreSQL 환경변수를 설정한다.

```bash
cp .env.example .env
```

기존 `.env` 파일이 있다면 그대로 사용한다.

## 2. PostgreSQL·Prometheus·Grafana 실행

프로젝트 루트에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

## 3. Backend 실행

프로젝트 루트의 별도 터미널에서 환경변수를 적용하고 실행한다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

## 4. 접속 주소

| 서비스 | 주소 |
|---|---|
| Backend 메트릭 | http://localhost:8080/actuator/prometheus |
| Prometheus | http://localhost:9090 |
| Prometheus 수집 상태 | http://localhost:9090/targets |
| Grafana | http://localhost:3000 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |

Prometheus Targets에서 `renegade`가 `UP`인지 확인한다.

## 5. Grafana 데이터 소스

최초 구성 시 Connections → Data sources에서 Prometheus를 추가한다.

- Prometheus server URL: `http://prometheus:9090`
- 인증: 없음
- Scrape interval: `15s`

Save & test로 연결을 확인한다. 같은 Compose 네트워크의 서비스 이름으로 연결한다.

대시보드 이름: `Renegade Backend Overview`

## 6. Simulator 실행

등록된 차량 ID를 `simulator/.env`에 설정한다.

```dotenv
API_BASE_URL=http://localhost:8080
VEHICLE_ID=1
SEND_INTERVAL_SECONDS=1
```

Day 4에서 구성한 가상환경을 활성화하고 실행한다.

```bash
cd simulator
source .venv/bin/activate
python main.py
```

`Ctrl+C`로 종료한다.

## 7. 주요 지표

| 지표 | 의미 |
|---|---|
| JVM 스레드 수 | 현재 살아 있는 JVM 스레드 수 |
| JVM 힙 메모리 | 현재 사용 중인 힙 메모리 |
| DB 커넥션 풀 | 활성·유휴 커넥션 수 및 커넥션 대기 스레드 수 |
| API 초당 요청 수 | `/api/` 경로의 초당 요청 수 |
| API 서버 오류율 | `/api/` 요청 중 HTTP 5xx 응답 비율 |
| API p95·p99 | 히스토그램으로 추정한 구간별 응답 시간 백분위 |
| GC 일시정지 시간 | 초당 GC 일시정지에 사용된 시간 |
| 텔레메트리 저장 결과 | 저장 트랜잭션의 성공·실패 누적 건수 |

요청이 없는 구간에서는 오류율과 응답 시간을 계산할 수 없어 그래프가 비어 있을 수 있다.

## 8. 저장 결과 집계 기준

메트릭 이름: `telemetry_save_total`

- `result="success"`: 저장 트랜잭션 커밋 완료
- `result="failure"`: 저장 트랜잭션 롤백 완료
- 존재하지 않는 차량으로 인한 롤백도 실패에 포함
- Controller 입력 검증에서 거절된 요청은 집계 제외
- Backend 재시작 시 카운터는 0부터 시작

## 9. 데이터 보관

- Prometheus 데이터: `prometheus-data` 볼륨
- Grafana 설정·대시보드: `grafana-data` 볼륨

컨테이너를 중지해도 볼륨 데이터는 유지된다. `docker compose down -v`는 PostgreSQL을 포함한 볼륨을 삭제하므로 데이터 유지가 필요할 때 사용하지 않는다.

## 10. 종료

Backend와 Simulator는 실행 터미널에서 각각 `Ctrl+C`로 종료한다.

컨테이너는 프로젝트 루트에서 중지한다.

```bash
docker compose stop
```