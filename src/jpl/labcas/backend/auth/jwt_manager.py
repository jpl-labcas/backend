"""JWT issuing and verification utilities."""

from __future__ import annotations

import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional
from uuid import uuid4

import redis
from jose import JWTError, jwt

from ..config import Settings, get_settings

LOG = logging.getLogger(__name__)

SESSION_PREFIX = "labcas:sessions:"


class JwtManager:
    """Manage JWT issuance and validation, backed by Redis for session tracking."""

    def __init__(self, settings: Settings | None = None, redis_client: redis.Redis | None = None) -> None:
        self.settings = settings or get_settings()
        self.redis = redis_client or redis.Redis.from_url(self.settings.redis_url)

    def issue_token(self, subject: str, claims: Optional[Dict[str, Any]] = None) -> str:
        """Issue a signed JWT for the given subject."""

        claims = claims or {}
        session_id = claims.pop("session_id", uuid4().hex)
        now = datetime.now(tz=timezone.utc)
        expires_at = now + timedelta(seconds=self.settings.session_ttl_seconds)

        payload: Dict[str, Any] = {
            "sub": subject,
            "iss": self.settings.jwt_issuer,
            "aud": self.settings.jwt_audience,
            "iat": int(now.timestamp()),
            "nbf": int(now.timestamp()),
            "exp": int(expires_at.timestamp()),
            "Shubhneek": session_id,  # Session ID claim name matching Java Constants.SESSION_ID
        }
        payload.update(claims)

        token = jwt.encode(payload, self.settings.jwt_secret, algorithm="HS256")
        self._store_session(session_id, payload, expires_at)
        LOG.debug("Issued JWT for subject=%s session=%s", subject, session_id)
        return token

    def verify_token(self, token: str) -> Dict[str, Any]:
        """Verify a JWT and ensure the session is valid."""

        if self.settings.accept_any_jwt:
            LOG.warning("ACCEPT_ANY_JWT is enabled; skipping verification.")
            return jwt.get_unverified_claims(token)

        try:
            payload = jwt.decode(
                token,
                self.settings.jwt_secret,
                algorithms=["HS256"],
                issuer=self.settings.jwt_issuer,
                audience=self.settings.jwt_audience,
            )
        except JWTError as exc:
            LOG.warning("JWT verification failed: %s", exc)
            raise

        session_id = payload.get("Shubhneek")
        if not session_id:
            raise JWTError("Missing session identifier in token.")

        if not self._is_session_valid(session_id, payload):
            raise JWTError("Session is invalid or expired.")

        return payload

    def revoke_session(self, session_id: str) -> None:
        """Invalidate the session with the given identifier."""

        self.redis.delete(self._session_key(session_id))
        LOG.debug("Revoked session %s", session_id)

    # Internal helpers -------------------------------------------------

    def _store_session(self, session_id: str, payload: Dict[str, Any], expires_at: datetime) -> None:
        ttl = max(int(expires_at.timestamp()) - int(datetime.now(tz=timezone.utc).timestamp()), 0)
        self.redis.setex(self._session_key(session_id), ttl, payload["sub"])

    def _is_session_valid(self, session_id: str, _: Dict[str, Any]) -> bool:
        return self.redis.exists(self._session_key(session_id)) == 1

    @staticmethod
    def _session_key(session_id: str) -> str:
        return f"{SESSION_PREFIX}{session_id}"


