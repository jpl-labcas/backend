package gov.nasa.jpl.edrn.labcas.tasks.nist;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * NIST execution task: it simply builds the Shiny server URL with the appropriate parameters
 * and adds it to the dataset-level metadata.
 * 
 * @author luca
 *
 */
public class NistExecuteTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(NistExecuteTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// print out configuration
		LOG.log(Level.INFO, "NistExecuteTaskInstance running with config="+config);
		Properties props = config.getProperties();
		String baseURL = props.getProperty("baseURL");
		LOG.log(Level.INFO, "Config baseUrl="+baseURL);
		File dataArchiveDir = FileManagerUtils.getDatasetArchiveDir(metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID),
				                                                    metadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID));
		// adda dataset version
		File dataDir = new File(dataArchiveDir, metadata.getMetadata(Constants.METADATA_KEY_VERSION));
		String appURL = baseURL + "?datadir="+dataDir;
		LOG.log(Level.INFO, "Constructed ApplicationURL="+appURL);
		metadata.addMetadata("ApplicationURL", appURL);
		
	}

}
