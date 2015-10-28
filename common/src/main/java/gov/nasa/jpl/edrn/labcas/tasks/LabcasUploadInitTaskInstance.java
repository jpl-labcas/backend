package gov.nasa.jpl.edrn.labcas.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import org.apache.commons.lang.WordUtils;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

/**
 * Task used to initialize a LabCAS crawler workflow.
 * o it creates or updates a product type for the dataset to be uploaded
 * o if found, it adds all metadata contained in the file DatasetMetadata.xml to the product type metadata
 * o it parses the target archive directory and automatically sets the dataset version
 * o cleans up the previously generated metadata files
 * 
 * @author luca
 *
 */
public class LabcasUploadInitTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUploadInitTaskInstance.class.getName());
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
		        
		// retrieve dataset name
		String dataset = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
		// enforce no spaces
		if (dataset.contains(" ")) {
			throw new WorkflowTaskInstanceException("Dataset name cannot contain spaces");
		}
		
		// build product type
		String productType = WordUtils.capitalize(dataset).replaceAll("\\s+", "_");
		//String productType = dataset;
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		Metadata datasetMetadata = Utils.readDatasetMetadata( dataset ) ;
		
		// transfer metadata field 'Description' to dataset description
		String datasetDescription = dataset; // default
		if (datasetMetadata.containsKey(Constants.METADATA_KEY_DESCRIPTION)) {
			datasetDescription = datasetMetadata.getMetadata(Constants.METADATA_KEY_DESCRIPTION);
			datasetMetadata.removeMetadata(Constants.METADATA_KEY_DESCRIPTION);
		}
		
		try {
			
			// create product type directory with the same name
			File datasetDir = Utils.getDatasetDir(dataset);
			File policyDir = new File(datasetDir, "policy");
			if (!policyDir.exists()) {
				policyDir.mkdirs();
			}

			// create file "elements.xml"
			File elementsXmlFile = new File(policyDir, "elements.xml");
			if (!elementsXmlFile.exists()) {
				Utils.makeElementsXmlFile( elementsXmlFile );
			}
			
			// create file "product-type-element-map.xml"
			File productTypeElementMapXmlFile = new File(policyDir, "product-type-element-map.xml");
			if (!productTypeElementMapXmlFile.exists()) {
				Utils.makeProductTypeElementMapXmlFile( productTypeElementMapXmlFile, productType );
			}
			
			// create "product-types.xml" (override each time)
			File productTypesXmlFile = new File(policyDir, "product-types.xml");
			Utils.makeProductTypesXmlFile(productTypesXmlFile, productType, datasetDescription, datasetMetadata);
			
			// must upload the same product type through the File Manager XML/RPC interface so it can be used right away
			// without waiting for the static XML metadtaa to be ingested at the next startup
			Utils.addProductType(productType, datasetDescription, datasetMetadata);
			
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
