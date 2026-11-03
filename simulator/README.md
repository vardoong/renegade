# Renegade Vehicle Simulator

Spring Boot 백엔드에 차량 주행 데이터를 주기적으로 전송하는 Python 시뮬레이터

## 주요 기능

- 정차 → 가속 → 정속 주행 → 감속 상태 반복
- 속도, RPM, 배터리 잔량, 온도, GPS 좌표 생성
- 설정된 주기로 `POST /api/v1/telemetry`에 데이터 전송
- 네트워크 오류 발생 시 시뮬레이터 실행 유지
- 서버 복구 후 자동 전송 재개

## 실행 방법

Python 3.13과 실행 중인 Renegade Spring Boot 서버가 필요합니다.

```bash
cd simulator
python3.13 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

`simulator/.env` 파일 설정:

```dotenv
API_BASE_URL=http://localhost:8080
VEHICLE_ID=1
SEND_INTERVAL_SECONDS=1
```

시뮬레이터 실행:

```bash
python main.py
```

`Ctrl+C`로 종료합니다.

## 테스트

```bash
python -m pytest -v
```

- 주행 상태 및 센서 데이터 검증
- API 정상 응답 및 예외 처리 검증
- 단위 테스트 14개 통과

## 참고 사항

- 실제 차량이 아닌 가상 주행 데이터를 생성합니다.
- 전송 실패 데이터는 재전송하지 않습니다.
- 연결 타임아웃은 최대 3회 시도하며, 중복 저장 위험이 있는 오류는 재시도하지 않습니다.