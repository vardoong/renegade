from unittest.mock import Mock, patch

import requests

from client import TelemetryClient


def test_send_success():
    """정상 응답 처리 검증"""
    client = TelemetryClient("http://localhost:8080")

    mock_response = Mock()
    mock_response.json.return_value = {"id": 1}

    with patch.object(client.session, "post", return_value=mock_response):
        result = client.send({"vehicleId": 1})

    assert result == {"id": 1}


def test_send_http_error():
    """HTTP 오류 발생 시 None 반환 검증"""
    client = TelemetryClient("http://localhost:8080")

    mock_response = Mock()
    mock_response.status_code = 404
    mock_response.text = "Vehicle not found"
    mock_response.raise_for_status.side_effect = requests.HTTPError(
        response=mock_response
    )

    with patch.object(client.session, "post", return_value=mock_response):
        result = client.send({"vehicleId": 999999})

    assert result is None


def test_send_read_timeout():
    """응답 타임아웃 발생 시 None 반환 검증"""
    client = TelemetryClient("http://localhost:8080")

    with patch.object(
            client.session,
            "post",
            side_effect=requests.ReadTimeout()
    ):
        result = client.send({"vehicleId": 1})

    assert result is None


def test_send_connection_error():
    """연결 오류 발생 시 None 반환 검증"""
    client = TelemetryClient("http://localhost:8080")

    with patch.object(
            client.session,
            "post",
            side_effect=requests.ConnectionError()
    ):
        result = client.send({"vehicleId": 1})

    assert result is None


@patch("client.time.sleep")
def test_send_connect_timeout_retry(mock_sleep):
    """연결 타임아웃 발생 시 최대 3회 시도하는지 검증"""
    client = TelemetryClient(
        "http://localhost:8080",
        max_attempts=3
    )

    with patch.object(
            client.session,
            "post",
            side_effect=requests.ConnectTimeout()
    ) as mock_post:
        result = client.send({"vehicleId": 1})

    assert result is None
    assert mock_post.call_count == 3
    assert mock_sleep.call_count == 2