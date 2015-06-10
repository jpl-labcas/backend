package gov.nasa.jpl.edrn.labcas.common.conditions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionInstance;

public class FileExistenceCondition implements WorkflowConditionInstance {
	
	private static final Logger LOG = Logger.getLogger(FileExistenceCondition.class.getName());

	@Override
	public boolean evaluate(Metadata metadata, WorkflowConditionConfiguration config) {
		
		for (String key : metadata.getAllKeys()) {
			LOG.info("==> key: ["+key+"]     value: ["+metadata.getMetadata(key)+"]");
		}
				
		// retrieve condition configuration
		String filepath = config.getProperty("filepath");
		int mustBeOlderThanInSecs = Integer.parseInt(config.getProperty("mustBeOlderThanInSecs"));

		// loop over expected output files
		Long now = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			
		File file = new File(filepath);
		Long fileLastModified = file.lastModified();
		
		LOG.info("Checking file: "+filepath+" last modified at:"+sdf.format(fileLastModified));
		
		// check existence
		if (!file.exists()) return false;
		
		// check last modified time
		if (fileLastModified > now - mustBeOlderThanInSecs*1000) return false;
						
		// file exists and is old enough
		return true;
		
	}

}
