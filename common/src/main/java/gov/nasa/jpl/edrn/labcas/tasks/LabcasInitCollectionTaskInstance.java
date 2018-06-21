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
 * 
 * Required metadata:
 * - CollectionName: generates ProductType with ' ' --> '_' substitution
 * - DatasetName: generates DatasetId with ' ' --> '_' substitution
 * - OwnerPrincipal
 * 
 * Optional metadata:
 * - CollectionDescription
 * - DatasetDescription
 * 
 * Other metadata from the request parameters becomes part of the Collection level (aka ProductType) metadata
 * (unless it starts with Dataset* in which case it becomes part of the Dataset-level metadata).
 * 
 * @author luca
 *
 */
public class LabcasInitCollectionTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasInitCollectionTaskInstance.class.getName());
		
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
						        
		try {
							
			// NOTE: "metadata" is global workflow metadata, passed on through the workflow tasks
			// on input, "metadata" contains (key, value) pairs supplied by client in XML/RPC invocation			
			LOG.info("LabcasInitCollectionTaskInstance: input collectionName="+metadata.getMetadata(Constants.METADATA_KEY_COLLECTION_NAME));
			LOG.info("LabcasInitCollectionTaskInstance: input datasetId="+metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID));
			LOG.info("LabcasInitCollectionTaskInstance: input datasetName="+metadata.getMetadata(Constants.METADATA_KEY_DATASET_NAME));
			
			// generate "ProductType" name from "CollectionName"
			String collectionName =  metadata.getMetadata(Constants.METADATA_KEY_COLLECTION_NAME);
			String productTypeName = collectionName.replaceAll("\\s+", "_");
			metadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName); // needed for file ingestion
			metadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_ID, productTypeName);
			
			// retrieve DatasetId, or generate it from "DatasetName"
			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			String datasetName = metadata.getMetadata(Constants.METADATA_KEY_DATASET_NAME);
			if (datasetId==null) {
				datasetId = datasetName.replaceAll("\\s+", "_");
				metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			}
			LOG.info("LabcasInitCollectionTaskInstance: using datasetId="+metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID));
			
			// populate product type metadata from XML/RPC parameters
			Metadata productTypeMetadata = new Metadata();
			productTypeMetadata.addMetadata(metadata);
			
			// populate dataset metadata
			Metadata datasetMetadata = new Metadata();
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetName); 
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_NAME, collectionName); 
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_COLLECTION_ID, productTypeName);
			
	        // optionally, add collection metadata from CollectionMetadata.xmlmet
	        Metadata _productTypeMetadata = FileManagerUtils.readCollectionMetadata(productTypeName);
	        productTypeMetadata.addMetadata(_productTypeMetadata);

	        // optionally, add dataset metadata from DatasetMetadata.xmlmet
	        Metadata _datasetMetadata = FileManagerUtils.readDatasetMetadata(productTypeName, datasetId);
	        datasetMetadata.addMetadata(_datasetMetadata);

			// transfer Dataset* metadata from collection-level to dataset-level
	        // do not override existing values
	        for (String key : productTypeMetadata.getAllKeys()) {
		        	if (key.startsWith("Dataset")) {
		        		// remove leading "Dataset:", if found
		        		String _key = key.replaceAll("Dataset:",""); 
		        		if (!datasetMetadata.containsKey(_key)) {
			        		for (String value : productTypeMetadata.getAllMetadata(key)) {
			        			datasetMetadata.addMetadata(_key, value);
			        		}
		        		}
		        		productTypeMetadata.removeMetadata(key);
		        	}
	        }
	        
	        // transfer OwnerPrincipal
	        LOG.info("Setting Dataset OwnerPrincipal to:"+metadata.getMetadata(Constants.METADATA_KEY_OWNER_PRINCIPAL));
	        if (metadata.containsKey(Constants.METADATA_KEY_OWNER_PRINCIPAL)) {
		        	datasetMetadata.addMetadata(Constants.METADATA_KEY_OWNER_PRINCIPAL, 
		        			                    metadata.getMetadata(Constants.METADATA_KEY_OWNER_PRINCIPAL));
	        }
	        
	        // create or update the Collection==ProductType metadata, unless otherwise indicated
	        boolean updateCollection = true;
	        if (metadata.containsKey(Constants.METADATA_KEY_UPDATE_COLLECTION)) {
	        		updateCollection = Boolean.parseBoolean(metadata.getMetadata(Constants.METADATA_KEY_UPDATE_COLLECTION).toLowerCase());
	        }

	        if (updateCollection) {
	        	
				// create or update the File Manager product type
				FileManagerUtils.createProductType(productTypeName, productTypeMetadata);
				
				// reload the catalog configuration so that the new product type is available for publishing
				FileManagerUtils.reload();
			
	        }
	        
	        // create or update the Dataset metadata, unless otherwise indicated
	        boolean updateDataset = true;
	        if (metadata.containsKey(Constants.METADATA_KEY_UPDATE_DATASET)) {
	        		updateDataset = Boolean.parseBoolean( metadata.getMetadata(Constants.METADATA_KEY_UPDATE_DATASET).toLowerCase() );
	        }
			
	        // add version to dataset metadata (if metadata flag "newVersion" is present)
	        //int datasetVersion = FileManagerUtils.getNextVersion( FileManagerUtils.findLatestDatasetVersion( productTypeName, datasetId ), metadata);
	        // FIXME
	        int datasetVersion = 1;
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+datasetVersion); // insert into dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_DATASET_VERSION, ""+datasetVersion);        // insert into product metadata
	        productTypeMetadata.removeMetadata(Constants.METADATA_KEY_NEW_VERSION);              // remove from collection metadata
	        	        	        
	        // set final product archive directory (same as set by LabcasProductVersioner)
	        metadata.replaceMetadata(Constants.METADATA_KEY_FILE_PATH, 
	        		                 FileManagerUtils.getDatasetArchiveDir(productTypeName, datasetId).getAbsolutePath());
				        
	        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
	        FileManagerUtils.cleanupStagingDir(productTypeName, datasetId);
	        
			// publish collection to the public Solr index
			// starting from the product type archive directory which contains the newly created "product-types.xml" file
	        if (updateCollection) {
	        		SolrUtils.publishCollection(FileManagerUtils.getProductTypeDefinitionDir(productTypeName));
	        }

			// publish dataset to the public Solr index
	        if (updateDataset) {
	        		SolrUtils.publishDataset(datasetMetadata);
	        }
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}
