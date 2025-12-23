"""Unit tests for JWT manager."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock, patch

import pytest
from jose import JWTError

from jpl.labcas.backend.auth.jwt_manager import JwtManager
from jpl.labcas.backend.config import Settings


@pytest.fixture
def mock_redis() -> MagicMock:
    """Create a mock Redis client."""
    redis_mock = MagicMock()
    redis_mock.setex = MagicMock()
    redis_mock.exists = MagicMock(return_value=1)
    redis_mock.delete = MagicMock()
    return redis_mock


@pytest.fixture
def test_settings() -> Settings:
    """Create test settings."""
    return Settings(
        jwt_secret="test-secret-key",
        jwt_issuer="test-issuer",
        jwt_audience="test-audience",
        session_ttl_seconds=3600,
        redis_url="redis://localhost:6379/0",
    )


def test_issue_token_creates_valid_token(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that issue_token creates a valid JWT."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    token = manager.issue_token("test-user")
    
    assert token is not None
    assert isinstance(token, str)
    assert len(token) > 0
    
    # Verify token can be decoded
    payload = manager.verify_token(token)
    assert payload["sub"] == "test-user"
    # Check against test_settings values (may be overridden by env)
    assert payload["iss"] == test_settings.jwt_issuer
    assert payload["aud"] == test_settings.jwt_audience
    assert "Shubhneek" in payload  # Session ID


def test_issue_token_stores_session(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that issue_token stores session in Redis."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    manager.issue_token("test-user")
    
    # Verify setex was called
    assert mock_redis.setex.called
    call_args = mock_redis.setex.call_args
    assert call_args is not None
    key, ttl, value = call_args[0]
    assert key.startswith("labcas:sessions:")
    assert value == "test-user"
    # TTL should be based on test_settings.session_ttl_seconds
    assert 0 < ttl <= test_settings.session_ttl_seconds


def test_issue_token_with_custom_claims(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that issue_token accepts custom claims."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    token = manager.issue_token("test-user", claims={"custom": "value", "another": 123})
    
    payload = manager.verify_token(token)
    assert payload["custom"] == "value"
    assert payload["another"] == 123


def test_verify_token_valid_token(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that verify_token validates a valid token."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    token = manager.issue_token("test-user")
    payload = manager.verify_token(token)
    
    assert payload["sub"] == "test-user"


def test_verify_token_invalid_token(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that verify_token rejects an invalid token."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    with pytest.raises(JWTError):
        manager.verify_token("invalid.token.here")


def test_verify_token_expired_token(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that verify_token rejects an expired token."""
    # Create a token with very short expiration
    from datetime import datetime, timedelta, timezone
    from jose import jwt
    
    now = datetime.now(tz=timezone.utc)
    expires_at = now - timedelta(seconds=1)  # Already expired
    
    payload = {
        "sub": "test-user",
        "iss": test_settings.jwt_issuer,
        "aud": test_settings.jwt_audience,
        "iat": int(now.timestamp()),
        "nbf": int(now.timestamp()),
        "exp": int(expires_at.timestamp()),
        "Shubhneek": "test-session-id",
    }
    
    token = jwt.encode(payload, test_settings.jwt_secret, algorithm="HS256")
    
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    # Token should be expired
    with pytest.raises(JWTError):
        manager.verify_token(token)


def test_verify_token_missing_session(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that verify_token rejects token when session is missing."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    token = manager.issue_token("test-user")
    
    # Make Redis return 0 (session doesn't exist)
    mock_redis.exists.return_value = 0
    
    with pytest.raises(JWTError, match="Session is invalid"):
        manager.verify_token(token)


def test_revoke_session(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that revoke_session deletes session from Redis."""
    manager = JwtManager(settings=test_settings, redis_client=mock_redis)
    
    manager.revoke_session("test-session-id")
    
    assert mock_redis.delete.called
    call_args = mock_redis.delete.call_args[0]
    assert call_args[0] == "labcas:sessions:test-session-id"


def test_verify_token_accept_any_jwt_enabled(test_settings: Settings, mock_redis: MagicMock) -> None:
    """Test that verify_token skips validation when ACCEPT_ANY_JWT is enabled."""
    from jose import jwt
    from jose.jwt import get_unverified_claims
    
    # Create a mock settings object to ensure accept_any_jwt is True
    class MockSettings:
        jwt_secret = "test-secret-key"
        jwt_issuer = "test-issuer"
        jwt_audience = "test-audience"
        session_ttl_seconds = 3600
        redis_url = "redis://localhost:6379/0"
        accept_any_jwt = True
    
    settings = MockSettings()  # type: ignore
    manager = JwtManager(settings=settings, redis_client=mock_redis)  # type: ignore
    
    # When accept_any_jwt is True, it uses jwt.get_unverified_claims
    # Create a token with a different secret (would normally fail verification)
    # but with accept_any_jwt=True, it should use get_unverified_claims which doesn't verify
    from datetime import datetime, timedelta, timezone
    now = datetime.now(tz=timezone.utc)
    payload_data = {
        "sub": "test-user",
        "exp": int((now + timedelta(hours=1)).timestamp()),
    }
    # Create token with different secret - normally this would fail verification
    # But get_unverified_claims should work regardless of signature
    token = jwt.encode(payload_data, "different-secret", algorithm="HS256")
    
    # Verify get_unverified_claims works directly
    unverified = get_unverified_claims(token)
    assert unverified["sub"] == "test-user"
    
    # With accept_any_jwt=True, manager.verify_token should use get_unverified_claims
    # Note: It still checks session validity, but get_unverified_claims doesn't require session
    payload = manager.verify_token(token)
    assert payload is not None
    assert payload["sub"] == "test-user"


def test_session_key_format() -> None:
    """Test that session keys are formatted correctly."""
    from jpl.labcas.backend.auth.jwt_manager import JwtManager
    
    key = JwtManager._session_key("test-session-id")
    assert key == "labcas:sessions:test-session-id"

