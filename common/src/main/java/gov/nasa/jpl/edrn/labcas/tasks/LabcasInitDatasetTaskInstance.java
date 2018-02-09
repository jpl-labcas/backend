package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.UUID;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;
import gov.nasa.jpl.edrn.labcas.utils.SolrUtils;

/**
 * Task that initializes the upload of a new Dataset within a given Collection (aka ProductType):
 * 
 * o the CollectionName name must be supplied as part of the task configuration metadata.
 * o the DatasetName is passed as part of the XML/RPC HTTP request - or is automatically generated if missing
 * 
 * @author luca
 *
 */
public class LabcasInitDatasetTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasInitDatasetTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// NOTE: "metadata" is global workflow metadata, passed on through the workflow tasks
		// eventually it becomes product i.e. file levele metadata
		
		try {
			
			// populate dataset metadata from workflow configuration and XML/RPC parameters
			Metadata datasetMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// generate "ProductType" name from "CollectionName"
			String collectionName =  datasetMetadata.getMetadata(Constants.METADATA_KEY_COLLECTION_NAME);
			String productTypeName = collectionName.replaceAll("\\s+", "_");
			metadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName);      // needed for file ingestion by OODT
			metadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_NAME, collectionName);
			metadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_ID, productTypeName);
			
			// generate "DatasetId" from "DatasetName", if passed through XML/RPC parameters
			// otherwise generate a UUID
			String datasetName = metadata.getMetadata(Constants.METADATA_KEY_DATASET_NAME);
			String datasetId = null;
			if (datasetName!=null) {
				datasetId = datasetName.replaceAll("\\s+", "_");
			} else {
				datasetId  = UUID.randomUUID().toString();
				datasetName = datasetId;
				metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetId); // datasetId == datasetName = UUID
			}
			metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			LOG.info("Using DatasetId="+datasetId);
			
			// populate dataset metadata
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetName); 
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_NAME, collectionName); 
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_ID, productTypeName);
			
	        // optionally, add dataset metadata from DatasetMetadata.xmlmet
	        Metadata _datasetMetadata = FileManagerUtils.readDatasetMetadata(productTypeName, datasetId);
	        datasetMetadata.addMetadata(_datasetMetadata);

										        
	        // add  version to dataset metadata (used for generating product unique identifiers)
	        int datasetVersion = FileManagerUtils.getNextVersion( FileManagerUtils.findLatestDatasetVersion( productTypeName, datasetId ), metadata);
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+datasetVersion); // dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+datasetVersion);        // product metadata
	        	        
	        // set final product archive directory (same as set by LabcasProductVersioner)
	        metadata.replaceMetadata(Constants.METADATA_KEY_FILE_PATH, 
	        		                 FileManagerUtils.getDatasetArchiveDir(productTypeName, datasetId, datasetVersion).getAbsolutePath());
	        
			// publish dataset to public Solr index
			SolrUtils.publishDataset(datasetMetadata);
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}