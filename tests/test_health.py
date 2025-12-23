from fastapi.testclient import TestClient

from jpl.labcas.backend.main import create_app


def test_healthcheck():
    app = create_app()
    client = TestClient(app)

    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


