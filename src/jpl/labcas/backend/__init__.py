"""
Python implementation of the LabCAS Data Access API.

Modules within this package are organized to mirror the original Java service
while providing abstractions for directory services, search engines, storage
providers, and application services.
"""

import importlib.resources

__all__ = ["__version__"]

__version__ = VERSION = importlib.resources.files(__name__).joinpath('VERSION.txt').read_text().strip()
