"""Functional tests for authentication endpoints."""

from __future__ import annotations

import base64
from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import SecurityContext, require_authenticated_user
from jpl.labcas.backend.auth.jwt_manager import JwtManager
from jpl.labcas.backend.directory.mock import MockDirectoryProvider
from jpl.labcas.backend.main import create_app


@pytest.fixture
def mock_directory() -> MockDirectoryProvider:
    """Create a mock directory provider."""
    directory = MockDirectoryProvider()
    directory.add_user("testuser", "testpass", "uid=testuser,ou=users,dc=example,dc=com")
    return directory


@pytest.fixture
def mock_jwt_manager() -> MagicMock:
    """Create a mock JWT manager."""
    manager = MagicMock(spec=JwtManager)
    manager.issue_token = MagicMock(return_value="test-jwt-token")
    manager.verify_token = MagicMock(return_value={"sub": "uid=testuser,ou=users,dc=example,dc=com"})
    manager.revoke_session = MagicMock()
    return manager


def test_auth_endpoint_with_form_data(mock_directory: MockDirectoryProvider, mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/auth endpoint with form data."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    # Override directory and JWT manager
    from jpl.labcas.backend.auth.dependencies import get_directory_provider, get_jwt_manager
    app.dependency_overrides[get_directory_provider] = lambda: mock_directory
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.post(
        "/data-access-api/auth",
        data={"username": "testuser", "password": "testpass"},
    )
    
    assert response.status_code == 200
    assert response.text == "test-jwt-token"
    assert response.headers["content-type"].startswith("text/plain")
    mock_jwt_manager.issue_token.assert_called_once()


def test_auth_endpoint_with_basic_auth(mock_directory: MockDirectoryProvider, mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/auth endpoint with Basic auth."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_directory_provider, get_jwt_manager
    app.dependency_overrides[get_directory_provider] = lambda: mock_directory
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    credentials = base64.b64encode(b"testuser:testpass").decode("utf-8")
    response = client.post(
        "/data-access-api/auth",
        headers={"Authorization": f"Basic {credentials}"},
    )
    
    assert response.status_code == 200
    assert response.text == "test-jwt-token"
    mock_jwt_manager.issue_token.assert_called_once()


def test_auth_endpoint_invalid_credentials(mock_directory: MockDirectoryProvider, mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/auth endpoint with invalid credentials."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_directory_provider, get_jwt_manager
    app.dependency_overrides[get_directory_provider] = lambda: mock_directory
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.post(
        "/data-access-api/auth",
        data={"username": "testuser", "password": "wrongpass"},
    )
    
    assert response.status_code == 401
    assert "Invalid username or password" in response.json()["detail"]


def test_auth_endpoint_missing_credentials(mock_directory: MockDirectoryProvider, mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/auth endpoint with missing credentials."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_directory_provider, get_jwt_manager
    app.dependency_overrides[get_directory_provider] = lambda: mock_directory
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.post("/data-access-api/auth")
    
    assert response.status_code == 401
    assert "Authentication required" in response.json()["detail"]


def test_logout_endpoint_success(mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/logout endpoint with valid session."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_jwt_manager
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.get(
        "/data-access-api/logout",
        params={"sessionID": "test-session-id"},
    )
    
    assert response.status_code == 200
    assert response.json()["status"] == "logged out"
    assert response.json()["message"] == "Session invalidated"
    mock_jwt_manager.revoke_session.assert_called_once_with("test-session-id")


def test_logout_endpoint_requires_authentication() -> None:
    """Test /data-access-api/logout endpoint requires authentication."""
    app = create_app()
    client = TestClient(app)
    
    response = client.get(
        "/data-access-api/logout",
        params={"sessionID": "test-session-id"},
    )
    
    assert response.status_code == 401


def test_logout_endpoint_missing_session_id(mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/logout endpoint with missing session ID."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_jwt_manager
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.get("/data-access-api/logout")
    
    assert response.status_code == 422  # Validation error


def test_auth_endpoint_invalid_basic_auth_format(mock_directory: MockDirectoryProvider, mock_jwt_manager: MagicMock) -> None:
    """Test /data-access-api/auth endpoint with invalid Basic auth format."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="uid=testuser,ou=users,dc=example,dc=com", groups=[]
    )
    
    from jpl.labcas.backend.auth.dependencies import get_directory_provider, get_jwt_manager
    app.dependency_overrides[get_directory_provider] = lambda: mock_directory
    app.dependency_overrides[get_jwt_manager] = lambda: mock_jwt_manager
    
    client = TestClient(app)
    
    response = client.post(
        "/data-access-api/auth",
        headers={"Authorization": "Basic invalid-base64"},
    )
    
    assert response.status_code == 401

