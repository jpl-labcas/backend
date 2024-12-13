# encoding: utf-8

'''Retrieve event IDs given a blinded site ID.

To run this, for example:

   `events-by-blind qfP7OH9pjawWGA`

This will display all event IDS for the blinded site ID `qfP7OH9pjawWGA` (which is part of `Prostate_MRI`).

or:

    `events-by-blind ldytNSGnHnrBQ`

This will display all event IDs for `ldytNSGnHnrBQ` (which is part of `Lung_Team_Project_2`).
'''

from ._argparse import add_standard_options
from ._credentials import get_credentials_from_args
from ._solr import connect_to_solr, find_documents_with_query
import argparse, logging, sys

_logger = logging.getLogger(__name__)


def main():
    '''Main entrypoint.

    Parses the command-line arguments, queries Solr for files that have the given
    `BlindedSiteID`, and prints the matching `eventID`s.
    '''    
    parser = argparse.ArgumentParser(description=__doc__)
    add_standard_options(parser)
    parser.add_argument('siteid', help='Blinded site ID, such as ')
    args = parser.parse_args()
    logging.basicConfig(level=args.loglevel, format='%(levelname)s %(message)s')
    creds = get_credentials_from_args(args)
    solr = connect_to_solr(creds, 'files')
    siteid = args.siteid

    # The Solr query `q` parameter looks for `BlindedSiteID:XYZ` where `XYZ` is the
    # blinded site identifier and all non-empty `eventID` fields.
    query = f'BlindedSiteID:{siteid} AND eventID:[* TO *]'

    # Start off with an empty set of event IDs.
    event_ids = set()

    # Go through each file that matches our `query`.
    for matching_file in find_documents_with_query(solr, query, ['eventID']):
        event_id = matching_file.get('eventID')[0]

        # If there's no event ID, skip it; this shouldn't happen given our query, but
        # it's a safe coding practice just in case.
        if not event_id: continue

        # Add it to the set.
        event_ids.add(event_id)

    # Turn the set into a list so we can sort it.
    event_ids = sorted(list(event_ids))

    # Show the values.
    _logger.info('For blinded site ID %s we have %d event IDs', siteid, len(event_ids))
    for event_id in event_ids:
        print(event_id)

    # That's it.
    sys.exit(0)

    # FYI, an even faster way to do this is to have Solr do the heavy lifting:
    #
    # https://edrn-labcas.jpl.nasa.gov/data-access-api/files/select?q=BlindedSiteID:XYZ%20AND%20eventID:%5B*%20TO%20*%5D&facet=true&facet.field=eventID&wt=json


if __name__ == '__main__':
    main()
