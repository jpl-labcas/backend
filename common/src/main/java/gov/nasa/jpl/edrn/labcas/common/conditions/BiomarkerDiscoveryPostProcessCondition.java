package gov.nasa.jpl.edrn.labcas.common.conditions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionInstance;

public class BiomarkerDiscoveryPostProcessCondition implements WorkflowConditionInstance {
	
	private static final Logger LOG = Logger.getLogger(BiomarkerDiscoveryPostProcessCondition.class.getName());

	@Override
	public boolean evaluate(Metadata metadata, WorkflowConditionConfiguration config) {
		
		for (String key : metadata.getAllKeys()) {
			LOG.info("==> key: ["+key+"]     value: ["+metadata.getMetadata(key)+"]");
		}
		
		// retrieve job-level metadata
		int ncv = Integer.parseInt( metadata.getMetadata("ncv") );
		
		// retrieve condition configuration
		String projectDirectory = config.getProperty("projectDirectory");
		String prefix = config.getProperty("prefix");

		// loop over expected output files
		Long now = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		for (int i=1; i<=ncv; i++) {
			
			String filepath = projectDirectory+"/results/cv/"+prefix+"_iter_"+i+".txt";
			File file = new File(filepath);
			Long fileLastModified = file.lastModified();
			
			LOG.info("Checking file: "+filepath+" last modified at:"+sdf.format(fileLastModified));
			
			// check existence
			if (!file.exists()) return false;
						
		}
		
		// all files exist
		return true;
	}

}