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
 * o it uses the task configuration parameter of the form "init.field...." to define the produt type metadata
 * o if found, it adds all metadata fields contained in the file DatasetMetadata.xml to the above product type metadata
 * o it parses the target archive directory and determines the dataset version
 * o it augments the file level metadata with the product type name and dataset version 
 *   (the dataset identifier already comes from the workflow invocation)
 * o cleans up the previously generated metadata files
 * 
 * @author luca
 *
 */
public class LabcasUploadInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		        
		try {
			
			// retrieve dataset identifier
			String datasetId = metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
			// enforce no spaces
			if (datasetId.contains(" ")) {
				throw new WorkflowTaskInstanceException("DatasetId cannot contain spaces");
			}
			
			// populate core dataset metadata
			Metadata coreMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
						
			// update dataset object in File Manager
			String productTypeName = FileManagerUtils.uploadDataset(datasetId, coreMetadata);
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();
			
	        // set the next dataset version into products metadata
	        int version = FileManagerUtils.findLatestDatasetVersion( datasetId ) + 1;
	        LOG.fine("Setting next dataset version to: "+version);
	        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);
	        	        
	        // set the ProductType into products metadata
	        metadata.replaceMetadata(Constants.PRODUCT_TYPE, productTypeName);
	                        
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
