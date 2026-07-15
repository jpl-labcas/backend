"""Unit tests for security utilities."""

from __future__ import annotations

import pytest

from jpl.labcas.backend.utils.security import ensure_safe_value


def test_ensure_safe_value_allows_safe_characters() -> None:
    """Test that safe characters are allowed."""
    assert ensure_safe_value("test123") == "test123"
    assert ensure_safe_value("hello_world") == "hello_world"
    assert ensure_safe_value("field:value") == "field:value"
    assert ensure_safe_value("test-value") == "test-value"
    assert ensure_safe_value("test.value") == "test.value"
    assert ensure_safe_value("test@value") == "test@value"
    assert ensure_safe_value("test#value") == "test#value"
    assert ensure_safe_value("test&value") == "test&value"
    assert ensure_safe_value("test*value") == "test*value"
    assert ensure_safe_value("test+value") == "test+value"
    assert ensure_safe_value("test=value") == "test=value"
    assert ensure_safe_value("test!value") == "test!value"
    assert ensure_safe_value("test?value") == "test?value"
    assert ensure_safe_value("test^value") == "test^value"
    assert ensure_safe_value("test|value") == "test|value"
    assert ensure_safe_value("test~value") == "test~value"
    assert ensure_safe_value("test[value]") == "test[value]"
    assert ensure_safe_value("test{value}") == "test{value}"
    assert ensure_safe_value("test(value)") == "test(value)"


def test_ensure_safe_value_rejects_unsafe_characters() -> None:
    """Test that unsafe characters are rejected."""
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test>value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test<value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test%value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test$value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value('test"value')
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test'value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test`value")


def test_ensure_safe_value_rejects_multiple_unsafe_characters() -> None:
    """Test that multiple unsafe characters are detected."""
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test<>value")
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        ensure_safe_value("test%$value")


def test_ensure_safe_value_empty_string() -> None:
    """Test that empty string is allowed."""
    assert ensure_safe_value("") == ""


def test_ensure_safe_value_unicode() -> None:
    """Test that unicode characters are allowed."""
    assert ensure_safe_value("test_测试") == "test_测试"
    assert ensure_safe_value("test_émoji") == "test_émoji"

