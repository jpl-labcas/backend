package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;
import gov.nasa.jpl.edrn.labcas.utils.SolrUtils;

/**
 * Task that initializes the upload of a new data Collection (aka ProductType) into LabCAS. 
 * The following core metadata fields must be supplied as part of the XML/RPC invocation:
 * - the ProductType title --> used to generate the ProductType name and id
 * - the ProductType description
 * - the DatasetId (typically equal to the directory name)
 * Other ProductType metadata is also populated from the request parameters.
 * 
 * @author luca
 *
 */
public class LabcasInitCollectionTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasInitCollectionTaskInstance.class.getName());
		
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
				        
		try {
			
			// retrieve metadata from XML/RPC parameters
			String title =  metadata.getMetadata(Constants.METADATA_KEY_TITLE);
			String productTypeName = title.replaceAll("\\s+", "_");
			if (productTypeName.contains(" ")) {  // enforce no spaces
				throw new WorkflowTaskInstanceException("ProductType cannot contain spaces");
			}
			metadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName); // needed for file ingestion

			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			if (datasetId.contains(" ")) {  // enforce no spaces
				throw new WorkflowTaskInstanceException("DatasetId cannot contain spaces");
			}
			
			// populate product type metadata from workflow configuration and XML/RPC parameters
			Metadata productTypeMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// remove DatasetId from product type metadata
			productTypeMetadata.removeMetadata(Constants.METADATA_KEY_DATASET_ID);
			
			// create or update the File Manager product type
			FileManagerUtils.createProductType(productTypeName, productTypeMetadata);
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();
			
			// publish collection to the Solr index
			// starting from the product type archive directory which also contains the "product-types.xml" file
			SolrUtils.publishCollections( FileManagerUtils.getProductTypeArchiveDir(productTypeName) );

			// populate dataset metadata
			Metadata datasetMetadata = new Metadata();
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetId); // dataset name == dataset id
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName);
			
	        // add  version to dataset metadata (used for generating product unique identifiers)
	        int version = FileManagerUtils.getNextVersion( FileManagerUtils.findLatestDatasetVersion( productTypeName, datasetId ), metadata);
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+version); // dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+version);        // product metadata
	        
	        // set final product archive directory (same as set by LabcasProductVersioner)
	        metadata.replaceMetadata(Constants.METADATA_KEY_FILE_PATH, 
	        		                 FileManagerUtils.getProductTypeArchiveDir(productTypeName)
	        		                 +"/"+datasetId+"/"+version+"/");

						
			// copy all product type metadata to product metadata
	        for (String key : productTypeMetadata.getAllKeys()) {
	        	if (!metadata.containsKey(key)) {
	        		LOG.fine("==> Copy metadata for key="+key+" from dataset-level to file-level.");
	        		metadata.addMetadata(key, productTypeMetadata.getAllMetadata(key));
	        	}
	        }
				        
	        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
	        FileManagerUtils.cleanupStagingDir(datasetId);
	        
			// publish dataset to public Solr index
			SolrUtils.publishDataset(datasetMetadata);
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}
