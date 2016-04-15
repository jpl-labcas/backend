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
 * Task used to initialize a LabCAS crawler workflow 
 * to upload a new dataset, or a new version of a dataset.
 * 
 * o it creates or updates a product type for the dataset to be uploaded
 * o it uses the task configuration parameter of the form "init.field...." to define the product type metadata
 * o if found, it adds all metadata fields contained in the file DatasetMetadata.xmlmet to the above product type metadata
 * o it parses the target archive directory and determines the dataset version
 * o it augments the file level metadata with the dataset id and version
 * o cleans up the previously generated metadata files
 * 
 * @author luca
 *
 */
public class LabcasUploadInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// debug: print all workflow instance metadata
        for (String key : metadata.getAllKeys()) {
        	for (String val : metadata.getAllMetadata(key)) {
        		LOG.fine("==> Input metadata key="+key+" value="+val);
        	}
        }
		        
		try {
			
			// retrieve dataset identifier
			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			// enforce no spaces
			if (datasetId.contains(" ")) {
				throw new WorkflowTaskInstanceException("DatasetId cannot contain spaces");
			}
			
			// populate product type metadata from workflow configuration
			Metadata productTypeMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// override parent product type from XML/RPC parameters
			if (metadata.containsKey(Constants.METADATA_KEY_PARENT_DATASET_ID)) {
				productTypeMetadata.replaceMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID, metadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID));

			// or read from workflow configuration
			} else if (productTypeMetadata.containsKey(Constants.METADATA_KEY_PARENT_DATASET_ID)) {
				metadata.replaceMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID, productTypeMetadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID));
				
			// or set default
			} else {
				productTypeMetadata.addMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID, Constants.ECAS_PRODUCT_TYPE);
				metadata.replaceMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID, productTypeMetadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID));
			}
			
	        // add dataset version to product type metadata (used for generating product unique identifiers)
			String parentDatasetId = productTypeMetadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID);
	        int version = FileManagerUtils.findLatestDatasetVersion( datasetId, parentDatasetId );
	        if (version==0) {  // dataset does not yet exist -> assign first version
	        	version = 1; 
	        } else {              // keep the same version unless the flag is set
	        	if (Boolean.parseBoolean(metadata.getMetadata(Constants.METADATA_KEY_NEW_VERSION))) {
	        		version += 1; // increment version
	        		metadata.removeMetadata(Constants.METADATA_KEY_NEW_VERSION); // remove the flag
	        	}
	        }
	        productTypeMetadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version); // product type metadata
	        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);            // workflow (-> poroduct) metadata
	        LOG.fine("Using dataset version=: "+version);

			// create or update the File Manager product type
			String productTypeName = FileManagerUtils.uploadDataset(datasetId, productTypeMetadata);
			metadata.replaceMetadata(Constants.PRODUCT_TYPE, productTypeName); // transfer to product level metadata
			
			// copy all product type metadata to product metadata
	        for (String key : productTypeMetadata.getAllKeys()) {
	        	if (!metadata.containsKey(key)) {
	        		LOG.fine("==> Copy metadata for key="+key+" from dataset-level to file-level.");
	        		metadata.addMetadata(key, productTypeMetadata.getAllMetadata(key));
	        	}
	        }
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();
	                        
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
