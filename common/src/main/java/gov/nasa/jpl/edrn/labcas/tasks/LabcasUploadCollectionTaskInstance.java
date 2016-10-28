package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
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
			
			// populate dataset metadata from workflow configuration and XML/RPC parameters
			Metadata productTypeMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// create or update the File Manager product type
			// FIXME
			LOG.info("About to create product type: "+productTypeName);
			String _productTypeName = FileManagerUtils.uploadProductType(productTypeName, productTypeMetadata);
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();

			// populate dataset metadata
			// FIXME: change to id, version ?
			Metadata datasetMetadata = new Metadata();
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_ID, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_DATASET_NAME, datasetId);
			datasetMetadata.replaceMetadata(Constants.METADATA_KEY_PRODUCT_TYPE, productTypeName);
			
	        // add  version to dataset metadata (used for generating product unique identifiers)
			//String parentDatasetId = datasetMetadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID);
	        //int version = FileManagerUtils.findLatestDatasetVersion( datasetId, parentDatasetId );
	        // FIXME
	        int version = 0;
	        if (version==0) {  // dataset does not yet exist -> assign first version
	        	version = 1; 
	        } else {              // keep the same version unless the flag is set
	        	if (Boolean.parseBoolean(metadata.getMetadata(Constants.METADATA_KEY_NEW_VERSION))) {
	        		version += 1; // increment version
	        		metadata.removeMetadata(Constants.METADATA_KEY_NEW_VERSION); // remove the flag
	        	}
	        }
	        datasetMetadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version); // dataset metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);        // product metadata
	        LOG.fine("Using dataset version=: "+version);

						
			// copy all product type metadata to product metadata
	        for (String key : productTypeMetadata.getAllKeys()) {
	        	if (!metadata.containsKey(key)) {
	        		LOG.fine("==> Copy metadata for key="+key+" from dataset-level to file-level.");
	        		metadata.addMetadata(key, productTypeMetadata.getAllMetadata(key));
	        	}
	        }
			
	                        
	        // FIXME: common functionality
	        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
	        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + datasetId;
	        String[] metFiles = new File(stagingDir).list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).getAbsolutePath().endsWith(Constants.OODT_METADATA_EXTENSION);
	                  }
	                });
	        for (String metFile : metFiles) {
	        	File _metFile = new File(stagingDir, metFile);
	        	LOG.fine("Deleting older metadata file: "+_metFile.getAbsolutePath());
	        	_metFile.delete();
	        }
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}
