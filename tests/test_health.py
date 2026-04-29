from fastapi.testclient import TestClient

from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.main import create_app


def test_healthcheck():
    app = create_app()
    client = TestClient(app)

    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_request_header_logging_redacts_sensitive_values(capsys):
    app = create_app(Settings(log_request_headers=True))
    client = TestClient(app)

    response = client.get(
        "/health",
        headers={
            "Authorization": "Bearer test-jwt-token",
            "Cookie": "token=test-jwt-token; JasonWebToken=test-jwt-token",
            "X-Debug": "visible",
        },
    )

    assert response.status_code == 200
    log_output = capsys.readouterr().err
    assert "Incoming request headers" in log_output
    assert "Bearer <redacted>" in log_output
    assert "names=token,JasonWebToken" in log_output
    assert "visible" in log_output
    assert "test-jwt-token" not in log_output


