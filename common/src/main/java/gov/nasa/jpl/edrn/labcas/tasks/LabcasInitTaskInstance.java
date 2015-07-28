package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

/**
 * Task used to initialize a LabCAS workflow.
 * For now it does nothing except printing stetements.
 * 
 * @author luca
 *
 */
public class LabcasInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		
		// print out configuration
		LOG.log(Level.INFO, "LabcasInitTaskInstance running with config="+config);
		Properties props = config.getProperties();
		Enumeration e = props.propertyNames();
	    while (e.hasMoreElements()) {
	      String key = (String) e.nextElement();
	      LOG.log(Level.INFO, "\t==> config key=["+key+"] value=["+props.getProperty(key)+"]");
	    }
	    
	    // print out metadata
	    LOG.log(Level.INFO, "LabcasInitTaskInstance using metadata="+metadata);
		for (String key : metadata.getAllKeys()) {
			LOG.info("\t==> metadata key=["+key+"] value=["+metadata.getMetadata(key)+"]");
		}

		
		
	}

}
