package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Task used to initialize a LabCAS crawler workflow.
 * It parses the target archive directory and automatically sets the dataset version.
 * 
 * @author luca
 *
 */
public class CrawlerInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(CrawlerInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// archive/dataset directory
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/crawl"; // FIXME ?
        File datasetDir = new File(archiveDir, metadata.getMetadata(Constants.METADATA_KEY_DATASET));
        
        // set the next "Version" metadata attribute
        String version = "1"; // default version
        if (datasetDir.exists()) {
            
	        LOG.fine("Looking for dataset versions in "+datasetDir.getAbsolutePath());
	
	        // list "version" sub-directories
	        String[] directories = datasetDir.list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).isDirectory();
	                  }
	                });
	        version = Integer.toString(directories.length+1);      	
	        
        }
        
        LOG.fine("Setting next dataset version to: "+version);
        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, version);
		
	}

}
