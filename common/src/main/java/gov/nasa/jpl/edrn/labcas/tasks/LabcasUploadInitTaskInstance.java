package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

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
		        
		// dataset name
		String dataset = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
		
        // set the next dataset version
        int version = Utils.findLatestDatasetVersion( dataset ) + 1;
        LOG.fine("Setting next dataset version to: "+version);
        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);
        
        // add global dataset metadata from file DatasetMetadata.xml
        metadata.addMetadata( Utils.readDatasetMetadata( dataset ) );
                
        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + dataset;
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
