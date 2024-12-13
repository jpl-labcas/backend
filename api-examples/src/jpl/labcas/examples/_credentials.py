# encoding: utf-8

'''Handling the EDRN username and password.'''


import argparse, os, sys, getpass


def get_credentials_from_args(args: argparse.Namespace) -> tuple[str, str]:
    '''Get the credentials from the parsed command-line arguments.

    Note that the `--username` "option" is _required_, so this will abort the program
    if it's not given. You can also set the `EDRN_USERNAME` environment variable to
    avoid having to do this.

    The `--password` option can be given to provide a password, but please note that
    this is not safe as command-lines are visible to other users on the system. You can
    provide the EDRN_PASSWORD environent variable instead. However, to be safest, if
    neither are given, this will prompt you for your password.

    This function returns a tuple of the username and password.
    '''
    username = args.username
    if not username:
        username = os.getenv('EDRN_USERNAME')
        if not username:
            print('You must supply the --username argument or set the EDRN_USERNAME env var')
            sys.exit(-1)
    password = args.password
    if not password:
        password = os.getenv('EDRN_PASSWORD')
        if not password:
            password = getpass.getpass(f'Password for {username}: ')
    return username, password
