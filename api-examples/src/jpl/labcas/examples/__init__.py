# encoding: utf-8

'''JPL LabCAS examples for the Solr API in the Data Access API.'''

import importlib.resources

PACKAGE_NAME = __name__
__version__ = VERSION = importlib.resources.files(__name__).joinpath('VERSION.txt').read_text().strip()
