package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * Task that initializes the upload of a new data Collection (aka ProductType) into LabCAS. 
 * The following core metadata fields must be supplied as part of the XML/RPC invocation:
 * - the ProductType name
 * - the ProductType description
 * - the DatasetId (typically equal to the directory name)
 * Other ProductType metadata is also populated from the request parameters.
 * 
 * @author luca
 *
 */
public class LabcasUploadCollectionTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadCollectionTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// debug: print all workflow instance metadata
        for (String key : metadata.getAllKeys()) {
        	for (String val : metadata.getAllMetadata(key)) {
        		LOG.fine("==> Input metadata key="+key+" value="+val);
        	}
        }
		        
		try {
			
			// retrieve metadata from XML/RPC parameters
			String productTypeName = metadata.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE);
			if (productTypeName.contains(" ")) {  // enforce no spaces
				throw new WorkflowTaskInstanceException("ProductType cannot contain spaces");
			}

			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			if (datasetId.contains(" ")) {  // enforce no spaces
				throw new WorkflowTaskInstanceException("DatasetId cannot contain spaces");
			}
			
			// populate product type metadata from workflow configuration and XML/RPC parameters
			Metadata productTypeMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// create or update the File Manager product type
			FileManagerUtils.createProductType(productTypeName, productTypeMetadata);
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();

			// populate dataset metadata
			Metadata datasetMetadata = new Metadata();
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetId); // dataset name == dataset id
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE_NAME, productTypeName);
			
	        // add  version to dataset metadata (used for generating product unique identifiers)
	        int version = FileManagerUtils.getNextVersion( FileManagerUtils.findLatestDatasetVersion( productTypeName, datasetId ), metadata);
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version); // dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);        // product metadata
						
			// copy all product type metadata to product metadata
	        for (String key : productTypeMetadata.getAllKeys()) {
	        	if (!metadata.containsKey(key)) {
	        		LOG.fine("==> Copy metadata for key="+key+" from dataset-level to file-level.");
	        		metadata.addMetadata(key, productTypeMetadata.getAllMetadata(key));
	        	}
	        }
				        
	        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
	        FileManagerUtils.cleanupStagingDir(datasetId);

		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}
