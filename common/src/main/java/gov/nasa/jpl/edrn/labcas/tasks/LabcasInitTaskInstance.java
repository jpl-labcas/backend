package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

/**
 * Task used to initialize a LabCAS workflow.
 * For now it does nothing except printing a stetement.
 * 
 * @author luca
 *
 */
public class LabcasInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		LOG.log(Level.INFO, "LabcasInitTaskInstance running with config="+config);
		
	}

}
