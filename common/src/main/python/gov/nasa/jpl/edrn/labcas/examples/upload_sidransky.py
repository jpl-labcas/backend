# Example Python script to uploas Sidransky data

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    # datasetId must match the directory name where the data is staged on the server: $LABCAS_STAGING/$datasetId
    datasetId = 'GSTP1_Methylation'
    labcasClient = LabcasClient()
        
    # product type metadata (to be submitted as part of upload workflow)
    metadata = { 
                 # required
                 'DatasetName':'GSTP1 Methylation',
                 'ProtocolId':'397',
                 'ProtocolName':'GSTP1 Methylation',
                 'LeadPI':'David Sidransky',
                 'DataCustodian':'David Sidransky',
                 'DataCustodianEmail':'dsidrans@jhmi.edu',
                 'CollaborativeGroup':'Prostate and Urologic',
                 'OwnerGroup':'/David/Sidransky',
                 # optional
                 'OrganSite':'Prostate',
                 'SiteName':'Johns Hopkins University (Biomarker Developmental Laboratories)',
                 'SiteShortName':'JHUSOM',
                 'QAState':'Accepted',
                 'Discipline':'Epigenomics',
    } 
    

    # upload dataset staged in directory 'mydata'
    labcasClient.uploadDataset(datasetId, metadata)
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(datasetId)
        
    # list all products for given dataset == product type
    labcasClient.listProducts(datasetId)