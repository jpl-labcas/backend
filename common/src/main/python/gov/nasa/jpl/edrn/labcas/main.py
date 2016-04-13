# Example script to query for a product type

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    labcasClient = LabcasClient()
    datasetName = "ERR164552"
    labcasClient.getProductTypeByName(datasetName)