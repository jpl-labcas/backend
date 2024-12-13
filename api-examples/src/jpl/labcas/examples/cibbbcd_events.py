# encoding: utf-8

'''Retrieve event IDs for the "Combined Imaging and Blood Biomarkers for Breast Cancer
Diagnosis" collection. This collection has datasets that have the following identification
format:

Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis/KIND/EVENT-ID/…

where KIND is either "Training" or "Validation" and EVENT-ID is the event identifier;
following that are specific subfolders to each event.

The output of this example is in CSV format (suitable for Numbers, Excel, Google Sheets,
etc.) that has the KIND in column A and the EVENT-ID in column B. Redirect the output
to a file if you wish.
'''

from ._argparse import add_standard_options
from ._credentials import get_credentials_from_args
from ._solr import connect_to_solr, find_documents_with_query
import argparse, logging, csv, sys, re

_logger = logging.getLogger(__name__)

# This is the only collection we're interested in:

_collection_id = 'Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis'


# Identifiers for datasets in CIBBBCD follow the form:
#
#     Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis/KIND/EVENT-ID/…
#
# We're only interested in the KIND and EVENT-ID, so we make a regular expression to
# match it:

_event_exp = re.compile(rf'^{_collection_id}/(Training|Validation)/([^/]+)$')


def main():
    '''Main entrypoint.

    Parses the command-line arguments, queries Solr for datasets that match, and
    writes them to the standard output in CSV format.
    '''    
    parser = argparse.ArgumentParser(description=__doc__)
    add_standard_options(parser)
    args = parser.parse_args()
    logging.basicConfig(level=args.loglevel, format='%(levelname)s %(message)s')
    creds = get_credentials_from_args(args)
    solr = connect_to_solr(creds, 'datasets')

    # Get our CSV writer ready and write out a "header" row.
    _logger.debug('Writing CSV header row')
    writer = csv.writer(sys.stdout)
    writer.writerow(['Kind', 'Event ID'])

    # Find all datasets that match our CIBBBCD collection. We're only interested in
    # the `id` field.
    for dataset in find_documents_with_query(solr, f'id:{_collection_id}/*', ['id']):
        # And we're only interested in the `id` field if it matches our regular expression.
        match = _event_exp.match(dataset['id'])
        if not match:
            _logger.debug('Dataset %s does not match, ignoring it', dataset['id'])
            continue

        # If we get here, it matches `Combined_…/KIND/EVENT-ID`, so write it out.
        kind, event_id = match.group(1), match.group(2)
        _logger.debug('MATCH found in %s; it has kind %s and event ID %s', dataset['id'], kind, event_id)
        writer.writerow([kind, event_id])

    # That's it.
    sys.exit(0)


if __name__ == '__main__':
    main()
