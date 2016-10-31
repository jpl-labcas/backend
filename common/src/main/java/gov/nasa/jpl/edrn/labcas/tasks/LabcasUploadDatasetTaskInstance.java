package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * Task that initializes the upload of a new Dataset within a given Collection (aka ProductType):
 * o the ProductType name must be supplied as part of the task configuration metadata.
 * o the DatasetId is passed as part of the XML/RPC HTTP request.
 * 
 * @author luca
 *
 */
public class LabcasUploadDatasetTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadDatasetTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		//FileManagerUtils.printMetadata(metadata);
		
		try {
			
			// populate dataset metadata from workflow configuration and XML/RPC parameters
			Metadata datasetMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// retrieve product type from configuration metadata
			// also needed at file-level metadata for ingestion
			String productTypeName = datasetMetadata.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE);
			metadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName); // transfer to product level metadata
			
			// retrieve dataset identifier from XML/RPC parameters
			// or generate a new unique one
			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			if (datasetId==null) {
				datasetId = UUID.randomUUID().toString();
				metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId); 
				datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId); 
			// enforce no spaces
			} else if (datasetId.contains(" ")) {
				throw new WorkflowTaskInstanceException("DatasetId cannot contain spaces");
			}
							        
	        // add  version to dataset metadata (used for generating product unique identifiers)
	        int version = FileManagerUtils.getNextVersion( FileManagerUtils.findLatestDatasetVersion( productTypeName, datasetId ), metadata);
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+version); // dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+version);        // product metadata
	        
			// copy all product type metadata to product metadata
	        for (String key : datasetMetadata.getAllKeys()) {
	        	if (!metadata.containsKey(key)) {
	        		LOG.fine("==> Copy metadata for key="+key+" from dataset-level to file-level.");
	        		metadata.addMetadata(key, datasetMetadata.getAllMetadata(key));
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
