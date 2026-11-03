import logging
import time

import requests


logger = logging.getLogger(__name__)


class TelemetryClient:

    def __init__(self, base_url: str, max_attempts: int = 3):
        self.url = f"{base_url.rstrip('/')}/api/v1/telemetry"
        self.session = requests.Session()
        self.max_attempts = max_attempts

    def send(self, telemetry: dict) -> dict | None:
        for attempt in range(1, self.max_attempts + 1):
            try:
                response = self.session.post(
                    self.url,
                    json=telemetry,
                    timeout=(3, 5)
                )

                response.raise_for_status()
                return response.json()

            except requests.exceptions.ConnectTimeout:
                logger.warning(
                    "Connection timeout: attempt=%d/%d",
                    attempt,
                    self.max_attempts
                )

            except requests.exceptions.ReadTimeout:
                logger.error(
                    "Read timeout: delivery status unknown"
                )
                return None

            except requests.exceptions.ConnectionError as exc:
                logger.warning(
                    "Connection error: attempt=%d/%d, error=%s",
                    attempt,
                    self.max_attempts,
                    exc
                )

                # ConnectionError에는 전송 이후 연결이 끊어진
                # 경우도 포함될 수 있으므로 무조건 재시도하면 안 됨.
                return None

            except requests.exceptions.HTTPError as exc:
                logger.error(
                    "HTTP error: status=%d, response=%s",
                    exc.response.status_code,
                    exc.response.text[:300]
                )
                return None

            except requests.exceptions.RequestException:
                logger.exception("Unexpected request error")
                return None

            if attempt < self.max_attempts:
                time.sleep(min(2 ** (attempt - 1), 5))

        logger.error("Telemetry delivery failed after retries")
        return None