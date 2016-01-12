package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;

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
	      String value = props.getProperty(key);
	      LOG.log(Level.INFO, "\t==> config key=["+key+"] value=["+value+"]");
	      
	      // check existsence of files/directory inputs
	      String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING);
	      if (value.equalsIgnoreCase("directory") || value.equalsIgnoreCase("file"))  {
	    	  String relPath = metadata.getMetadata(key);
	    	  File file = new File(stagingDir, relPath);
	    	  if (value.equalsIgnoreCase("file")) {
	    		  if (!file.exists()) {
	    			  throw new WorkflowTaskInstanceException("File "+relPath+" not found");
	    		  }
	    	  }
	    	  if (value.equalsIgnoreCase("directory")) {
	    		  if (!file.exists() || !file.isDirectory()) {
	    			  throw new WorkflowTaskInstanceException("Directory "+relPath+" not found.");
	    		  }
	    	  }
	      }
	      
	    }
	    
	    // print out metadata
	    LOG.log(Level.INFO, "LabcasInitTaskInstance using metadata="+metadata);
		for (String key : metadata.getAllKeys()) {
			LOG.info("\t==> metadata key=["+key+"] value=["+metadata.getMetadata(key)+"]");
		}

		
	}

}
