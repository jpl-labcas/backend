package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import org.apache.log4j.Level;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Task used to initialize a LabCAS crawler workflow.
 * o it parses the target archive directory and automatically sets the dataset version
 * o if found, it adds all metadata contained in the file DatasetMetadata.xml (to all products in the dataset)
 * 
 * @author luca
 *
 */
public class LabcasUploadInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// archive/dataset directory
		String workflowDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/" + Constants.WORKFLOW_LABCAS_UPOLOAD;
        File datasetDir = new File(workflowDir, metadata.getMetadata(Constants.METADATA_KEY_DATASET));
        
        // set the next "Version" metadata attribute
        // don't just count the directories, select the highest number
        int version = 0;
        if (datasetDir.exists()) {
            
	        LOG.fine("Looking for dataset versions in "+datasetDir.getAbsolutePath());
	
	        // list "version" sub-directories
	        String[] directories = datasetDir.list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).isDirectory();
	                  }
	                });
	        for (String dir : directories) {
	        	int v = Integer.parseInt(dir);
	        	if (v > version) version = v;
	        }
	        
        }
        
        // increment to next version
        version++;      	
        LOG.fine("Setting next dataset version to: "+version);
        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);
        
        // add global dataset metadata from file DatasetMetadata.xml
        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + metadata.getMetadata(Constants.METADATA_KEY_DATASET);
        File datasetMetadataFile = new File(stagingDir, Constants.METADATA_FILE);
        
        if (datasetMetadataFile.exists()) {
        	LOG.info("Adding metadata from file: "+datasetMetadataFile.getAbsolutePath());
        	
        	try {
        		 SerializableMetadata sm = new SerializableMetadata("UTF-8", false);
        		 sm.loadMetadataFromXmlStream(datasetMetadataFile.toURI().toURL().openStream());
        		 Metadata datasetMetadata = sm.getMetadata();
     			 for (String key : datasetMetadata.getAllKeys()) {
    				for (String val : datasetMetadata.getAllMetadata(key)) {
    					LOG.fine("\t==> Adding dataset metadata key=["+key+"] value=["+val+"]");
    				}
     			 }
        		 metadata.addMetadata(sm.getMetadata());
        		 
        	} catch (Exception e) {
        		LOG.warning(e.getMessage());
        	}
        	
        } else {
        	LOG.warning("Metadata file: "+datasetMetadataFile.getAbsolutePath()+" not found");
        }
        
        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
        // list "version" sub-directories
        String[] metFiles = new File(stagingDir).list(new FilenameFilter() {
                  @Override
                  public boolean accept(File current, String name) {
                    return new File(current, name).getAbsolutePath().endsWith(Constants.METADATA_EXTENSION);
                  }
                });
        for (String metFile : metFiles) {
        	File _metFile = new File(stagingDir, metFile);
        	LOG.fine("Deleting older metadata file: "+_metFile.getAbsolutePath());
        	_metFile.delete();
        }
		
	}

}
