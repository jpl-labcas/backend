package gov.nasa.jpl.edrn.labcas.tasks;

import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

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

		try {
						
			// retrieve dataset identifier
			String dataset = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
			
			// populate core dataset metadata
			Metadata coreMetadata = FileManagerUtils.readConfigMetadata(metadata, config);
			
			// update dataset object in File Manager
			FileManagerUtils.updateDataset(dataset, coreMetadata);
			
			// update products metadata directly into Solr
			FileManagerUtils.updateProducts(dataset);
			
			// reload the catalog configuration so that the new product type is available for publishing
			FileManagerUtils.reload();
			
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
			throw new WorkflowTaskInstanceException(e.getMessage());
		}
		
	}

}
