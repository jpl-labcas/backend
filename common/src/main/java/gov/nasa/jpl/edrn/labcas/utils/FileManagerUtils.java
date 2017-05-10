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
import org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor;
import gov.nasa.jpl.edrn.labcas.extractors.XmlFileMetExtractor;

/**
 * Class that contains common functionality to interact with the FileManager.
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
	 * Method to create the XML configuration for a new Product Type.
	 * 
	 * @param dataset
	 * @param coreMetadata
	 * @throws Exception
	 */
	public static void createProductType(String productTypeName, Metadata productTypeMetadata) throws Exception {
				
		// transfer metadata field 'Description' to product type description, if found
		String productTypeDescription = productTypeName; // default product type description = product type name
		if (productTypeMetadata.containsKey(Constants.METADATA_KEY_COLLECTION_DESCRIPTION)) {
			productTypeDescription = productTypeMetadata.getMetadata(Constants.METADATA_KEY_COLLECTION_DESCRIPTION);
			productTypeMetadata.removeMetadata(Constants.METADATA_KEY_COLLECTION_DESCRIPTION);
		}
		
		// create product type directory with the same name
		File productTypeDir = FileManagerUtils.getProductTypeDefinitionDir(productTypeName);
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
	public static int findLatestDatasetVersion(final String productTypeName, final String datasetId) {
		
		File productTypeDir = FileManagerUtils.getProductTypeArchiveDir(productTypeName);
		File datasetDir = new File(productTypeDir, datasetId);
		LOG.fine("Looking for dataset versions in "+datasetDir.getAbsolutePath());
		
        int version = 0;
        if (datasetDir.exists()) {      
	
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
        
        LOG.info("Current dataset version: "+version);
        return version;
		
	}
	
	/** 
	 * Method to possibly increment the dataset version based on the "NewVersion" flag.
	 * @param version
	 * @param metadata
	 * @return
	 */
	public static int getNextVersion(int version, Metadata metadata) {
        
		if (version==0) {  // dataset does not yet exist -> assign first version
        	version = 1; 
        } else {              // keep the same version unless the flag is set
        	if (Boolean.parseBoolean(metadata.getMetadata(Constants.METADATA_KEY_NEW_VERSION))) {
        		version += 1; // increment version
        		metadata.removeMetadata(Constants.METADATA_KEY_NEW_VERSION); // remove the flag
        	}
        }
        return version;
        
	}
	
	/**
	 * Method to cleanup the older .met files from the dataset staging directory.
	 * @param datasetId
	 */
	public static void cleanupStagingDir(String productTypeName, String datasetId) {
		
        // remove all .met files from staging directory - probably a leftover of a previous workflow submission
        // only if data was uploaded from staging directory (not generated by workflow)
		File stagingDir = getStagingDir(productTypeName, datasetId);
		if (stagingDir.exists()) {
	        LOG.fine("Cleaning up dataset staging directory: "+stagingDir.getAbsolutePath());
	        String[] metFiles = stagingDir.list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).getAbsolutePath().endsWith(Constants.OODT_METADATA_EXTENSION);
	                  }
	                });
	        for (String metFile : metFiles) {
	        	File _metFile = new File(stagingDir, metFile);
	        	LOG.fine("Deleting older metadata file: "+_metFile.getAbsolutePath());
	        	_metFile.delete();
	        }
		}
		
	}
	
	/**
	 * Utility method that reads the additional collection metadata 
	 * from the file CollectionMetadata.xmlmet located in the top-level collection staging directory.
	 * NOTE: currently not used.
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException
	 */
	public static Metadata readCollectionMetadata(String productTypeName) {
		
        File stagingDir = getStagingDir(productTypeName);
        File datasetMetadataFile = new File(stagingDir, Constants.COLLECTION_METADATA_FILE);
    
        return readMetadataFromFile(datasetMetadataFile);
        
	}
	
	/**
	 * Utility method that reads the additional dataset metadata 
	 * from the file DatasetMetadata.xmlmet located in the dataset staging directory.
	 * NOTE: currently not used.
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException
	 */
	public static Metadata readDatasetMetadata(String productTypeName, String datasetId) {
		
        File stagingDir = getStagingDir(productTypeName, datasetId);
        File datasetMetadataFile = new File(stagingDir, Constants.DATASET_METADATA_FILE);
    
        return readMetadataFromFile(datasetMetadataFile);
        
	}
	
	/**
	 * Utility method that reads metadata keys from the task configuration,
	 * and populates them with values from given metadata object. 
	 * @return
	 */
	public static Metadata readConfigMetadata(Metadata metadata, WorkflowTaskConfiguration config) {
		
		// new metadata object
		Metadata newMetadata = new Metadata();
        
        // extract metadata keys from task configuration parameters of the form "input.field...."
        // example of input metadata key: 
        // <property name="input.dataset.ProtocolId.type" value="integer" />
        // <property name="input.dataset.ProtocolId.title" value="Protocol ID" />
		// or fixed metadata key:
		// <property name="dataset.ParentDatasetId" value="NIST" />
		// <property name="CollectionName" value="NIST Product" />
        Set<String> newMetadataKeys = new HashSet<String>();
        for (Object objKey : config.getProperties().keySet()) {
            String key = (String) objKey;
            String value = config.getProperties().getProperty(key);
            LOG.fine("Workflow configuration property: key="+key+" value="+value);
            if (key.toLowerCase().startsWith("input.dataset.")) {
            	String[] parts = key.split("\\."); 
            	newMetadataKeys.add(parts[2]);
            } else if (key.toLowerCase().startsWith("dataset.")) {
            	String[] parts = key.split("\\."); 
            	newMetadata.addMetadata(parts[1], GeneralUtils.removeNonAsciiCharacters(value));
            } else {
            	newMetadata.addMetadata(key, GeneralUtils.removeNonAsciiCharacters(value));
            }
        }
        
        // populate new metadata values from client supplied metadata
        for (String key : newMetadataKeys) {
        	if (metadata.containsKey(key)) {
        		// Note: OODT split input metadata "My Data" as separate values "My", "Data"
        		// must merge the values back
        		List<String> values = metadata.getAllMetadata(key);
        		String value = StringUtils.join(values, " ");
        		newMetadata.addMetadata(key, GeneralUtils.removeNonAsciiCharacters(value));
        	}
        }
        
        return newMetadata;
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
	 * Retrieves the top-level directory where the product type policies are contained,
	 * currently implemented as $LABCAS_HOME/products
	 * 
	 * @param datasetName
	 * @return
	 */
	public static File getProductTypeDefinitionDir(final String productTypeName) {
		
		File productsDir = new File(System.getenv(Constants.ENV_LABCAS_HOME), "products");
		File productTypeDefDir = new File(productsDir, productTypeName); 
		return productTypeDefDir;
		
	}
	
	/**
	 * Retrieves the top-level directory where product type data is archived.
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
	 * Retrieves the directory where the dataset files are archived
	 * @param productTypeName
	 * @param datasetId
	 * @param datasetVersion
	 * @return
	 */
	public static File getDatasetArchiveDir(final String productTypeName, final String datasetId, final int datasetVersion) {
		
		String dirPath = getProductTypeArchiveDir(productTypeName) + "/" + datasetId + "/" + datasetVersion + "/";
		return new File(dirPath);
		
	}
	
	
	/**
	 * Retrieves the top-level dirtectory where data for a given collection must be staged.
	 * @param productTypeName
	 * @param datasetId
	 * @return
	 */
	public static File getStagingDir(final String productTypeName) {
		String dirPath = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + productTypeName;
		return new File(dirPath);
	}
	
	/**
	 * Retrieves the directory where the data for a given dataset must be uploaded.
	 * 
	 * @param productTypeName
	 * @param datasetId
	 * @return
	 */
	public static File getStagingDir(final String productTypeName, final String datasetId) {
		String dirPath = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + productTypeName + "/" + datasetId;
		return new File(dirPath);
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
                
        // <extractor class="org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor"> 
        //   <configuration>
        //      <property name="nsAware" value="true" />
        //      <property name="elementNs" value="CAS" />
        //      <property name="elements" value="ProductReceivedTime,ProductName,ProductId,ProductType,WorkflowName,DatasetId" />
        //   </configuration>
        // </extractor>
        Element labcasMetExtractorElement = xmlDocument.createElement("extractor");
        labcasMetExtractorElement.setAttribute("class", CoreMetExtractor.class.getCanonicalName());
        Element configElement = xmlDocument.createElement("configuration");
        makePropertyElement(xmlDocument, configElement, "nsAware", "true");
        makePropertyElement(xmlDocument, configElement, "elementNs", "CAS");
        makePropertyElement(xmlDocument, configElement, "elements", 
        		"ProductReceivedTime,ProductName,ProductId,ProductType,WorkflowName,DatasetId");
        labcasMetExtractorElement.appendChild(configElement);
        metExtractorsElement.appendChild(labcasMetExtractorElement);
        
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
	
	private static void makePropertyElement(Document xmlDocument, Element parentElement, 
			                                String elementAttributeName, String elementAttributeValue ) {
		
       	Element propertyElement = xmlDocument.createElement("property");
       	propertyElement.setAttribute(elementAttributeName, elementAttributeValue);
       	parentElement.appendChild(propertyElement);
       	
	}
	
	/**
	 * Utility method to print out the content of a metadata object.
	 * @param metadata
	 */
	public static void printMetadata(Metadata metadata) {

        for (String key : metadata.getAllKeys()) {
        	for (String val : metadata.getAllMetadata(key)) {
        		LOG.info("==> Metadata key="+key+" value="+val);
        	}
        }

	}

}
