# encoding: utf-8

'''Common argument parsing handling.'''

from . import VERSION
import argparse, logging


def _add_logging_argparse_options(parser: argparse.ArgumentParser):
    '''Add logging options to the given `parser`.'''
    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        '-d',
        '--debug',
        action='store_const',
        const=logging.DEBUG,
        default=logging.INFO,
        dest='loglevel',
        help='Log copious debugging messages suitable for developers',
    )
    group.add_argument(
        '-q',
        '--quiet',
        action='store_const',
        const=logging.WARNING,
        dest='loglevel',
        help="Don't log anything except warnings and critically-important messages",
    )


def _add_credential_argparse_options(parser: argparse.ArgumentParser):
    '''Add credential options to the given `parser`.'''
    parser.add_argument(
        '-u', '--username',
        help='EDRN username; defaults to value of the EDRN_USERNAME environment variable; either are *required*'
    )
    parser.add_argument(
        '-p', '--password',
        help="Password; defaults to EDRN_PASSWORD environment variable; if if not given, you'll be prompted for it"
    )


def add_standard_options(parser: argparse.ArgumentParser):
    '''Add the logging, credential, and `--version` options to the given `parser`.'''
    _add_logging_argparse_options(parser)
    _add_credential_argparse_options(parser)
    parser.add_argument('--version', action='version', version=f'%(prog)s {VERSION}')
