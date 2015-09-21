package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

/**
 * Task used to update the metadata of an already published dataset.
 * 
 * @author luca
 *
 */
public class LabcasUpdateTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUpdateTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// dataset name
		String datasetName = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
		LOG.info("Updating metadata for dataset: "+datasetName);
		
		// read dataset metadata
		Metadata datasetMetadata = Utils.readDatasetMetadata(datasetName);
		
	}

}
