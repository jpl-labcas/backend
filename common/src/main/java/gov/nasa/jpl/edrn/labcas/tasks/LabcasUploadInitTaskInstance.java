package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import org.apache.commons.lang.WordUtils;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.pge.writers.MetadataKeyReplacerTemplateWriter;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

/**
 * Task used to initialize a LabCAS crawler workflow.
 * o it parses the target archive directory and automatically sets the dataset version
 * o if found, it adds all metadata contained in the file DatasetMetadata.xml (to all products in the dataset)
 * o cleans up the previously generated metadata files
 * 
 * @author luca
 *
 */
public class LabcasUploadInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		        
		// retrieve dataset name == product type name
		String dataset = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
		//String productType = WordUtils.capitalize(dataset).replaceAll("\\s+", "_");
		String productType = dataset; // FIXME ?
		String datasetDescription = dataset; // FIXME ?
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		Metadata datasetMetadata = Utils.readDatasetMetadata( dataset ) ;
		
		// create product type directory with the same name
		File datasetDir = Utils.getDatasetDir(dataset);
		File policyDir = new File(datasetDir, "policy");
		if (!policyDir.exists()) {
			policyDir.mkdirs();
		}
		
		try {
			
			// create "elements.xml"
			File elementsXmlFile = new File(policyDir, "elements.xml");
			if (!elementsXmlFile.exists()) {
				Utils.makeElementsXmlFile( elementsXmlFile );
			}
			
			// create "product-type-element-map.xml"
			File productTypeElementMapXmlFile = new File(policyDir, "product-type-element-map.xml");
			if (!productTypeElementMapXmlFile.exists()) {
				Utils.makeProductTypeElementMapXmlFile( productTypeElementMapXmlFile, productType );
			}
			
			// create "product-types.xml" (override each time)
			File productTypesXmlFile = new File(policyDir, "product-types.xml");
			Utils.makeProductTypesXmlFile(productTypesXmlFile, productType, datasetDescription, datasetMetadata);
			
		} catch(Exception ioe) {
			throw new WorkflowTaskInstanceException(ioe.getMessage());
		}
		
		
        // set the next dataset version
        int version = Utils.findLatestDatasetVersion( dataset ) + 1;
        LOG.fine("Setting next dataset version to: "+version);
        metadata.replaceMetadata(Constants.METADATA_KEY_VERSION, ""+version);
        
        // add global dataset metadata from file DatasetMetadata.xml
        //metadata.addMetadata( );
        
        // set the ProductType
        metadata.replaceMetadata(Constants.PRODUCT_TYPE, productType);
                        
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
