package gov.nasa.jpl.edrn.labcas.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.extractors.XmlFileMetExtractor;

/**
 * Class that contains common functionality to interact with the FileManager.
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
	
	// parameters that enter into product type definition
	private static final String REPOSITORY = "file://[LABCAS_ARCHIVE]";
	private static final String VERSIONER = "gov.nasa.jpl.edrn.labcas.versioning.LabcasProductVersioner";
	
	private static final Logger LOG = Logger.getLogger(FileManagerUtils.class.getName());
	
	/**
	 * FIXME
	 * Method to upload a new collection to the File Manager, creating a corresponding product type;
	 * or, upload a new version of the same dataset, overriding the metadata for the same product type.
	 * @param dataset
	 * @param coreMetadata
	 * @throws Exception
	 */
	public static String uploadProductType(String productTypeName, Metadata productTypeMetadata) throws Exception {
				
		// build product type
		//String productTypeName = FileManagerUtils.getProductTypeName(dataset);
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		//Metadata datasetMetadata = FileManagerUtils.readDatasetMetadata( datasetId );
		
		// merge dataset specific metadata with core metadata
		//datasetMetadata.addMetadata(coreMetadata);
		
		// transfer metadata field 'Description' to product type description, if found
		String productTypeDescription = productTypeName; // default
		if (productTypeMetadata.containsKey(Constants.METADATA_KEY_DESCRIPTION)) {
			productTypeDescription = productTypeMetadata.getMetadata(Constants.METADATA_KEY_DESCRIPTION);
			productTypeMetadata.removeMetadata(Constants.METADATA_KEY_DESCRIPTION);
		}
		
		// create product type directory with the same name
		//String parentDataset = coreMetadata.getMetadata(Constants.METADATA_KEY_PARENT_DATASET_ID);
		File productTypeDir = FileManagerUtils.getProductTypeArchiveDir(productTypeName);
		LOG.info("Using product type archive dir="+productTypeDir);
		File policyDir = new File(productTypeDir, "policy");
		if (!policyDir.exists()) {
			policyDir.mkdirs();
		}
		
		// create file "elements.xml" (if not existing already)
		File elementsXmlFile = new File(policyDir, "elements.xml");
		if (!elementsXmlFile.exists()) {
			FileManagerUtils.makeElementsXmlFile( elementsXmlFile );
		}
		
		// create file "product-type-element-map.xml" (if not existing already)
		File productTypeElementMapXmlFile = new File(policyDir, "product-type-element-map.xml");
		if (!productTypeElementMapXmlFile.exists()) {
			String parentProductTypeName = Constants.LABCAS_PRODUCT_TYPE; // default parent product type
			FileManagerUtils.makeProductTypeElementMapXmlFile(productTypeElementMapXmlFile, productTypeName, parentProductTypeName);
		}

		// create "product-types.xml" (override each time)
		File productTypesXmlFile = new File(policyDir, "product-types.xml");
		FileManagerUtils.makeProductTypesXmlFile(productTypesXmlFile, productTypeName, productTypeDescription, productTypeMetadata);

		return productTypeName;

	}
		
	
	/**
	 * Method to instruct the File Manager to reload its policy configuration.
	 * @throws Exception
	 */
	public static void reload() throws Exception {
		
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(FILEMANAGER_URL));
		
		LOG.info("Reloading the File Manager...");
		boolean status = client.refreshConfigAndPolicy();

		// wait for the File Manager to come back online
		boolean isAlive = false;
		while (!isAlive) {
			try {
				isAlive = client.isAlive();	
			} catch(Exception e) {
				LOG.warning(e.getMessage());
				LOG.info("Waiting for File Manager to reload...");
				Thread.sleep(1000); // wait 1 second
			}
		}

		LOG.info("File Manager reoloaded - status="+status);
				
	}
	
	/** 
	 * Utility function to determine the latest version of an archived dataset.
	 * If not found, the latest version is set to 0.
	 */
	public static int findLatestDatasetVersion(final String datasetName, final String parentDatasetName) {
		
		File datasetDir = FileManagerUtils.getProductTypeArchiveDir(datasetName);
		
        int version = 0;
        if (datasetDir.exists()) {           
	        LOG.fine("Looking for dataset versions in "+datasetDir.getAbsolutePath());
	
	        // list "version" sub-directories
	        String[] directories = datasetDir.list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).isDirectory();
	                  }
	                });
	        // don't just count the directories, select the highest number
	        for (String dir : directories) {
	        	try { 
		        	int v = Integer.parseInt(dir);
		        	if (v > version) version = v;
	        	} catch(NumberFormatException e) {
	        		// skip "/policy" sub-directory
	        	}
	        }    
        }
        
        return version;
		
	}
	
	/**
	 * Utility method that reads the additional dataset metadata 
	 * from the file DatasetMetadata.xmlmet located in the dataset staging directory.
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException
	 */
	public static Metadata readDatasetMetadata(final String datasetName) {
		
        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + datasetName;
        File datasetMetadataFile = new File(stagingDir, Constants.METADATA_FILE);
    
        return readMetadataFromFile(datasetMetadataFile);
        
	}
	
	/**
	 * Utility method that reads the core dataset metadata from the task configuration,
	 * and populates it with values from the workflow instance metadata 
	 * (supplied when the workflow is submitted).
	 * @return
	 */
	public static Metadata readConfigMetadata(Metadata metadata, WorkflowTaskConfiguration config) {
		
		// product type metadata
		Metadata productTypeMetadata = new Metadata();
        
        // extract product type metadata keys from task configuration parameters of the form "init.field...."
        // example of input metadata key: 
        // <property name="input.dataset.ProtocolId.type" value="integer" />
        // <property name="input.dataset.ProtocolId.title" value="Protocol ID" />
		// or fixed metadata key:
		// <property name="dataset.ParentDatasetId" value="NIST" />
        Set<String> productTypeMetadataKeys = new HashSet<String>();
        for (Object objKey : config.getProperties().keySet()) {
            String key = (String) objKey;
            String value = config.getProperties().getProperty(key);
            LOG.fine("Workflow configuration property: key="+key+" value="+value);
            if (key.toLowerCase().startsWith("input.dataset.")) {
            	String[] parts = key.split("\\."); 
            	productTypeMetadataKeys.add(parts[2]);
            } else if (key.toLowerCase().startsWith("dataset.")) {
            	String[] parts = key.split("\\."); 
            	productTypeMetadata.addMetadata(parts[1], GeneralUtils.removeNonAsciiCharacters(value));
            }
        }
        
        // populate core dataset metadata values from client supplied metadata
       
        for (String key : productTypeMetadataKeys) {
        	if (metadata.containsKey(key)) {
        		// Note: OODT split input metadata "My Data" as separate values "My", "Data"
        		// must merge the values back
        		List<String> values = metadata.getAllMetadata(key);
        		String value = StringUtils.join(values, " ");
        		productTypeMetadata.addMetadata(key, GeneralUtils.removeNonAsciiCharacters(value));
        	}
        }
        
        return productTypeMetadata;
	}
	
	/**
	 * Utility method to read metadata from a file.
	 * 
	 * @param metadataFilepath
	 * @return
	 */
    public static Metadata readMetadataFromFile(final File metadataFilepath) {
		
		// read file metadata
        Metadata metadata = new Metadata(); // empty metadata container
        
        if (metadataFilepath.exists()) {
        	LOG.info("Reading metadata from file: "+metadataFilepath.getAbsolutePath());
        	
        	try {
        		 SerializableMetadata sm = new SerializableMetadata("UTF-8", false);
        		 sm.loadMetadataFromXmlStream(metadataFilepath.toURI().toURL().openStream());
        		 Metadata _metadata = sm.getMetadata();
     			 for (String key : _metadata.getAllKeys()) {
    				for (String val : _metadata.getAllMetadata(key)) {
    					LOG.fine("\t==> Read metadata key=["+key+"] value=["+val+"]");
    					// sanitize the metadata fields
    					metadata.addMetadata(key, GeneralUtils.removeNonAsciiCharacters(val) );
    				}
     			 }
        		 
        	} catch (Exception e) {
        		LOG.warning(e.getMessage());
        	}
        	
        } else {
        	LOG.warning("Metadata file: "+metadataFilepath.getAbsolutePath()+" not found");
        }
        
        return metadata;
		
	}
	
	/**
	 * Retrieves the directory where an uploaded product type will be archived.
	 * 
	 * @param datasetName
	 * @return
	 */
	public static File getProductTypeArchiveDir(final String productTypeName) {
		
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE);
		File productTypeDir = new File(archiveDir, productTypeName); 
		return productTypeDir;
		
	}
	
	
	/**
	 * Constructs the product type name from a dataset identifier.
	 * @param dataset
	 * @return
	 */
	private static String getProductTypeName(String dataset) {
		
		//return WordUtils.capitalize(dataset).replaceAll("\\s+", "_");
		return dataset; // product type name == dataset identifier
		
	}
	
	/**
	 * Utility method to create the file "elements.xml".
	 * @param filepath
	 * @throws Exception
	 */
	private static final void makeElementsXmlFile(File filepath) throws Exception {

		// XML document
		Document xmlDocument = XmlUtils.newXmlDocument();
        
        // <cas:elements xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":elements");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // write out the file
        XmlUtils.xmlToFile(xmlDocument, filepath);

	}
	
	/**
	 * Utility method to create the file "product-type-element-map.xml".
	 * @param filepath
	 * @param productType
	 * @throws Exception
	 */
	private static final void makeProductTypeElementMapXmlFile(File filepath, String productType, String parentProductType) throws Exception {
		
		// XML document
		Document xmlDocument = XmlUtils.newXmlDocument();
		
		// <cas:producttypemap xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":producttypemap");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // <type id="urn:edrn:Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set" parent="urn:edrn:LabcasProduct" />
        Element typeElement = xmlDocument.createElement("type");
        typeElement.setAttribute("id", Constants.EDRN_PREFIX+productType);
        typeElement.setAttribute("parent", Constants.EDRN_PREFIX+parentProductType);
        rootElement.appendChild(typeElement);

        // write out the file
        XmlUtils.xmlToFile(xmlDocument, filepath);
		
	}
	
	/**
	 * Utility method to create the file "product-types.xml".
	 * @param filepath
	 * @param productType
	 * @param description
	 * @param metadata
	 * @throws Exception
	 */
	private static final void makeProductTypesXmlFile(File filepath, String productTypeName, String description, Metadata metadata) throws Exception {
		
		// XML document
		Document xmlDocument = XmlUtils.newXmlDocument();
		
		// <cas:producttypes xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":producttypes");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // <type id="urn:oodt:Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set" name="Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set">
        Element typeElement = xmlDocument.createElement("type");
        typeElement.setAttribute("id", "urn:edrn:"+productTypeName);
        typeElement.setAttribute("name", productTypeName);
        rootElement.appendChild(typeElement);
        
        // <repository path="file://[LABCAS_ARCHIVE]"/>
        Element repositoryElement = xmlDocument.createElement("repository");
        repositoryElement.setAttribute("path", REPOSITORY);
        typeElement.appendChild(repositoryElement);
        
        // <versioner class="gov.nasa.jpl.edrn.labcas.versioning.LabcasProductVersioner"/>
        Element versionerElement = xmlDocument.createElement("versioner");
        versionerElement.setAttribute("class", VERSIONER);
        typeElement.appendChild(versionerElement);
        
        // <description>Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set product type</description>
        Element descriptionElement = xmlDocument.createElement("description");
        descriptionElement.insertBefore(xmlDocument.createTextNode(description), descriptionElement.getLastChild());
        typeElement.appendChild(descriptionElement);
        
        // <metExtractors>
        Element metExtractorsElement = xmlDocument.createElement("metExtractors");
        typeElement.appendChild(metExtractorsElement);
                
        // <extractor class="gov.nasa.jpl.edrn.labcas.extractors.XmlFileMetExtractor">
        Element extractor3Element = xmlDocument.createElement("extractor");
        extractor3Element.setAttribute("class", XmlFileMetExtractor.class.getCanonicalName());
        metExtractorsElement.appendChild(extractor3Element);
        
        // <configuration>
        Element configuration3Element = xmlDocument.createElement("configuration");
        extractor3Element.appendChild(configuration3Element);

        // <metadata>
        Element metadataElement = xmlDocument.createElement("metadata");
        typeElement.appendChild(metadataElement);
       
        // loop over all metadata keys
        for (String key : metadata.getAllKeys()) {

        	// <keyval>
        	Element keyvalElement = xmlDocument.createElement("keyval");
        	metadataElement.appendChild(keyvalElement);
        	
        	// <key>Dataset</key>
        	Element keyElement = xmlDocument.createElement("key");
        	keyElement.insertBefore(xmlDocument.createTextNode(key), keyElement.getLastChild());
        	keyvalElement.appendChild(keyElement);
        	
        	// loop over all values for that key
        	for (String val : metadata.getAllMetadata(key)) {
        	
	        	// <val>[DatasetId]</val>
	        	Element valElement = xmlDocument.createElement("val");
	        	valElement.insertBefore(xmlDocument.createTextNode(val), valElement.getLastChild());
	        	keyvalElement.appendChild(valElement);  
	        	
	        }
        	
        }

        // write out the file
        XmlUtils.xmlToFile(xmlDocument, filepath);
		
	}

}
