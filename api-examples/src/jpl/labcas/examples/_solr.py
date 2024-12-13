# encoding: utf-8

'''Convenience methods for accessing Solr.'''


from pysolr import Solr
from typing import Generator
import logging

_logger = logging.getLogger(__name__)
_rows = 10000
_endpoint = 'https://edrn-labcas.jpl.nasa.gov/data-access-api'


def connect_to_solr(credentials: tuple[str, str], kind: str) -> Solr:
    '''Connect to the EDRN LabCAS Solr API with the given `credentials` for the given `kind` of data.

    The `kind` should be `collections`, `datasets`, or `files`. This returns a Python Solr
    object ready for use.
    '''
    if kind not in ('collections', 'datasets', 'files'):
        raise ValueError(f'Unexpected kind «{kind}»')
    url = f'{_endpoint}/{kind}'
    return Solr(url, auth=credentials)


def find_documents_with_query(solr: Solr, query: str, fields: list[str] = []) -> Generator[dict, None, None]:
    '''Find documents in `solr` matching the given `query` and yielding only `fields`.

    If you want all fields, omit `fields` or pass an empty list.

    This handles Solr pagination for you; all you have to do is iterate over the yielded
    results, which are always dictionaries with keys as the requested fields and values
    which may be:

    - Single text strings, such as for the `id`, `CollectionId`, `FileSize`, etc., fields
    - Single-item lists of text strings, frequently for fields like `Institution`, `eventID`, etc.
    - Multi-item lists of text strings, typically for `OwnerPrincipal`
    '''
    start = 0
    while True:
        if fields:
            results = solr.search(q=query, start=start, rows=_rows, fl=','.join(fields))
        else:
            results = solr.search(q=query, start=start, rows=_rows)
        num_results = len(results)
        if num_results == 0:
            _logger.debug('At start %d with rows %d got zero results for %s, so done!', start, _rows, query)
            return
        start += num_results
        for result in results:
            yield result
