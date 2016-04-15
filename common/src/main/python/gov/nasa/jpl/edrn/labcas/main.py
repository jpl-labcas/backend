# Example script to query for a product type

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    labcasClient = LabcasClient()
    labcasClient.getProductTypeByName("RnaSeqProduct")
    labcasClient.getProductTypeByName("ERR164552")