# Example Python script to upload Hanash data

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    # datasetId must match the directory name where the data is staged on the server: $LABCAS_STAGING/$datasetId
    datasetId = 'FHCRCHanashAnnexinLamr'
    labcasClient = LabcasClient()
        
    # product type metadata (to be submitted as part of upload workflow)
    metadata = { 
                 # required
                 'DatasetName':'Autoantibody Biomarkers',
                 'ProtocolId':'138',
                 'ProtocolName':'Validation of Protein Markers for Lung Cancer Using CARET Sera and Proteomics Techniques',
                 'LeadPI':'Samir Hanash',
                 'DataCustodian':'Ji Qiu',
                 'DataCustodianEmail':'djiqiu@fhcrc.org',
                 'CollaborativeGroup':'Lung and Upper Aerodigestive',
                 'OwnerPrincipal':'/Samir/Hanash',
                 # optional
                 'OrganSite':'Lung',
                 'SiteName':'Fred Hutchinson Cancer Research Center (Biomarker Developmental Laboratories)',
                 'SiteShortName':'FHCRC',
                 'QAState':'Accepted',
                 'PubMedId':'http://www.ncbi.nlm.nih.gov/pubmed/18794547',
                 'DateDatasetFrozen':'2007/05/29',
    } 
    

    # upload dataset staged in directory 'mydata'
    labcasClient.uploadDataset(datasetId, metadata)
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(datasetId)
        
    # list all products for given dataset == product type
    labcasClient.listProducts(datasetId)