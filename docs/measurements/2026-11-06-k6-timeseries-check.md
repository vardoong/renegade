# Day 8 — k6 시계열 저장 확인 (2026-11-06)

텔레메트리 API에 50 req/s로 10초간 요청을 보내고, `measure.py`가 저장한 압축 시계열을 읽었다.

- HTTP 요청 501건. 시계열의 `http_reqs` 합계가 `summary.json`과 일치했다.
- 성공 체크 501건, HTTP 실패 0건.
- 요청별 HTTP 응답·대기·연결 시간과 반복 전체 시간, VU 표본 10개에 타임스탬프가 포함됐다.
- `dropped_iterations`는 시계열과 요약에 나타나지 않았다. 이번 실행에서는 누락 미관측으로 기록한다.

원본은 [실행 결과 폴더](../../load-test/results/20261106-140453-timeseries-check/)의 `k6-metrics.json.gz`와 `summary.json`에 있다. 결과 폴더는 Git에서 제외된다.

다음 작업은 요청 수·지연·VU·누락을 1초 단위로 집계하고 서버 지표와 시간축을 맞추는 것이다.
