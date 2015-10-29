package gov.nasa.jpl.edrn.labcas;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.WordUtils;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.filemgr.util.XmlStructFactory;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Class that contains common functionality to interact with the FileManager
 * both through its XML/RPC interface (for dynamic updates) 
 * and its XML repository (for persisting the updates to XML).
 * 
 * @author luca
 *
 */
public class FileManagerUtils {
	
	// default value for FileManager URL
	private static String FILEMANAGER_URL = "http://localhost:9000/";
	static {
		if (System.getenv(Constants.ENV_FILEMGR_URL)!=null) {
			FILEMANAGER_URL = System.getenv(Constants.ENV_FILEMGR_URL);
		}
	}
	
	private static final Logger LOG = Logger.getLogger(FileManagerUtils.class.getName());
		
	/**
	 * Method to add a new dataset, or update an existing dataset, into the File Manager.
	 * 
	 * @param dataset
	 */
	public static void updateDataset(String dataset) throws Exception {
		
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(FILEMANAGER_URL));
		
		// build product type from dataset
		String productTypeName = FileManagerUtils.getProductTypeName(dataset);
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		Metadata datasetMetadata = FileManagerUtils.readDatasetMetadata( dataset );
		
				
		// transfer metadata field 'Description' to dataset description
		String datasetDescription = dataset; // default
		if (datasetMetadata.containsKey(Constants.METADATA_KEY_DESCRIPTION)) {
			datasetDescription = datasetMetadata.getMetadata(Constants.METADATA_KEY_DESCRIPTION);
			datasetMetadata.removeMetadata(Constants.METADATA_KEY_DESCRIPTION);
		}
		
		// query File Manager for product type object
		ProductType productType = client.getProductTypeByName(productTypeName);
		LOG.info("Retrieved product type id="+productType.getProductTypeId()+" from File Manager");
		
		// update the product type object
		//productType.setDescription(datasetDescription);
		
        // loop over all metadata keys, values found in DatasetMetadata.xml
		Metadata typeMetadata = productType.getTypeMetadata();
        for (String key : datasetMetadata.getAllKeys()) {
        	LOG.info("Updating key="+key);
        	
        	// remove this metadata key
        	typeMetadata.removeMetadata(key);
        	
        	// insert all new (non-empty) values, overriding old values
        	List<String> vals = new ArrayList<String>();
        	for (String value : datasetMetadata.getAllMetadata(key)) {
        		if (value.trim().length()>0) {
        			LOG.info("Adding value="+value+" to key="+key);
        			typeMetadata.addMetadata(key, value);
        		}
        	}

        }
        
		// write the updated product type object to XML
		final List<ProductType> producTypes = Arrays.asList( new ProductType[] { productType });
		for (ProductType pt : producTypes) {
			LOG.info("Updating product type="+pt.getName());
		}
		File productTypesXmlFile = new File(FileManagerUtils.getDatasetDir(dataset), "/policy/product-types.xml");
		XmlStructFactory.writeProductTypeXmlDocument(producTypes, productTypesXmlFile.getAbsolutePath());
		LOG.info("Written XML file="+ productTypesXmlFile.getAbsolutePath());
		
		// send updated object to the File Manager
		String productTypeId = client.addProductType(productType);
		LOG.info("Updated product type="+productTypeId);
		
	}
	
	
	/**
	 * Utility method that reads the additional dataset metadata 
	 * from the file DatasetMetadata.xml located in the dataset staging directory.
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException
	 */
	private static Metadata readDatasetMetadata(final String datasetName) {
		
        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + datasetName;
        File datasetMetadataFile = new File(stagingDir, Constants.METADATA_FILE);
		
		// read input metadata
        Metadata metadata = new Metadata(); // empty metadata container
        if (datasetMetadataFile.exists()) {
        	LOG.info("Updating metadata from file: "+datasetMetadataFile.getAbsolutePath());
        	
        	try {
        		 SerializableMetadata sm = new SerializableMetadata("UTF-8", false);
        		 sm.loadMetadataFromXmlStream(datasetMetadataFile.toURI().toURL().openStream());
        		 metadata = sm.getMetadata();
     			 for (String key : metadata.getAllKeys()) {
    				for (String val : metadata.getAllMetadata(key)) {
    					LOG.fine("\t==> Read dataset metadata key=["+key+"] value=["+val+"]");
    				}
     			 }
        		 
        	} catch (Exception e) {
        		LOG.warning(e.getMessage());
        	}
        	
        } else {
        	LOG.warning("Metadata file: "+datasetMetadataFile.getAbsolutePath()+" not found");
        }
        
        return metadata;
		
	}
	
	/**
	 * Retrieves the directory where an uploaded dataset will be archived.
	 * 
	 * @param datasetName
	 * @return
	 */
	private static File getDatasetDir(final String datasetName) {
		
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/" + Constants.WORKFLOW_LABCAS_UPOLOAD;
		File datasetDir = new File(archiveDir, datasetName); 
		return datasetDir;
		
	}
	
	/**
	 * Constructs the product type name from a dataset identifier.
	 * @param dataset
	 * @return
	 */
	private static String getProductTypeName(String dataset) {
		
		String productType = WordUtils.capitalize(dataset).replaceAll("\\s+", "_");
		return productType;
		
	}

}
