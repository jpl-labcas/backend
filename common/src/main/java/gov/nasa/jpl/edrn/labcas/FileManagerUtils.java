package gov.nasa.jpl.edrn.labcas;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor;
import org.apache.oodt.cas.filemgr.metadata.extractors.examples.MimeTypeExtractor;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.filemgr.util.XmlStructFactory;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.nasa.jpl.edrn.labcas.extractors.XmlFileMetExtractor;
import gov.nasa.jpl.edrn.labcas.utils.SolrUtils;

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
	private static final String REPOSITORY = "file://[LABCAS_ARCHIVE]/labcas-upload";
	private static final String VERSIONER = "gov.nasa.jpl.edrn.labcas.versioning.LabcasProductVersioner";
	private final static String ELEMENT_LIST = "ProductReceivedTime,ProductName,ProductId,ProductType";
	
	private static final Logger LOG = Logger.getLogger(FileManagerUtils.class.getName());
	
	/**
	 * Method to upload a new dataset to the File Manager, creating a corresponding product type;
	 * or, upload a new version of the same dataset, overriding the metadata for the same product type.
	 * @param dataset
	 * @throws Exception
	 */
	public static String uploadDataset(String dataset) throws Exception {
		
		// build product type
		String productTypeName = FileManagerUtils.getProductTypeName(dataset);
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		Metadata datasetMetadata = FileManagerUtils.readDatasetMetadata( dataset );
		
		// transfer metadata field 'Description' to dataset description, if found
		String datasetDescription = dataset; // default
		if (datasetMetadata.containsKey(Constants.METADATA_KEY_DESCRIPTION)) {
			datasetDescription = datasetMetadata.getMetadata(Constants.METADATA_KEY_DESCRIPTION);
			datasetMetadata.removeMetadata(Constants.METADATA_KEY_DESCRIPTION);
		}
		
		// create product type directory with the same name
		File datasetDir = FileManagerUtils.getDatasetArchiveDir(dataset);
		File policyDir = new File(datasetDir, "policy");
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
			FileManagerUtils.makeProductTypeElementMapXmlFile( productTypeElementMapXmlFile, productTypeName );
		}

		// create "product-types.xml" (override each time)
		File productTypesXmlFile = new File(policyDir, "product-types.xml");
		FileManagerUtils.makeProductTypesXmlFile(productTypesXmlFile, productTypeName, datasetDescription, datasetMetadata);

		return productTypeName;

	}
		
	/**
	 * Method to update an existing dataset into the File Manager,
	 * i.e. to change the product type metadata without uploading new products.
	 * 
	 * @param dataset
	 */
	public static String updateDataset(String dataset) throws Exception {
		
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(FILEMANAGER_URL));
		
		// build product type from dataset
		String productTypeName = FileManagerUtils.getProductTypeName(dataset);
		
		// retrieve additional dataset metadata from file DatasetMetadata.xml
		Metadata datasetMetadata = FileManagerUtils.readDatasetMetadata( dataset );
				
		// query File Manager for product type object
		ProductType productType = client.getProductTypeByName(productTypeName);
		LOG.info("Retrieved product type id="+productType.getProductTypeId()+" from File Manager");
				
        // loop over all metadata keys, values found in DatasetMetadata.xml
		Metadata typeMetadata = productType.getTypeMetadata();
        for (String key : datasetMetadata.getAllKeys()) {
        	
        	// transfer "Description" metadata field to product type description
        	if (key.equals(Constants.METADATA_KEY_DESCRIPTION)) {
        		productType.setDescription(datasetMetadata.getMetadata(key));
        		
        	} else {
        	
	        	// remove this metadata key
	        	typeMetadata.removeMetadata(key);
	        	
	        	// insert all new (non-empty) values, overriding old values
	        	for (String value : datasetMetadata.getAllMetadata(key)) {
	        		if (value.trim().length()>0) {
	        			LOG.info("Adding value="+value+" to key="+key);
	        			typeMetadata.addMetadata(key, value);
	        		}
	        	}
        	}

        } // loop over all metadata keys, values
        
		// write the updated product type object to XML
		final List<ProductType> productTypes = Arrays.asList( new ProductType[] { productType });

		File productTypesXmlFile = new File(FileManagerUtils.getDatasetArchiveDir(dataset), "/policy/product-types.xml");
		XmlStructFactory.writeProductTypeXmlDocument(productTypes, productTypesXmlFile.getAbsolutePath());
		LOG.info("Written update product type metadata to XML file: "+ productTypesXmlFile.getAbsolutePath());
				
		return productTypeName;
	}
	
	/**
	 * Method to update the metadata content of all products belonging to a given dataset
	 * (latest version only).
	 * @param dataset
	 * @throws Exception
	 */
	public static void updateProducts(String dataset) throws Exception {
		
		// determine latest dataset version
		int version = findLatestDatasetVersion(dataset);
		
		// loop over .xmlmet files in staging directory
		File stagingDir = getDatasetStagingDir(dataset);
        String[] xmlmetFiles = stagingDir.list(new FilenameFilter() {
                  @Override
                  public boolean accept(File current, String name) {
                    return name.endsWith(Constants.EDRN_METADATA_EXTENSION);
                  }
                });
        
        
        // loop over products with additional metadata
        HashMap<String, Metadata> updateMetadataMap = new HashMap<String, Metadata>();
        for (String xmlmetFile : xmlmetFiles) {
        	if (!xmlmetFile.equals(Constants.METADATA_FILE)) {
        	
	        	// filename
	        	String filename = xmlmetFile.replace(Constants.EDRN_METADATA_EXTENSION, "");
	        	// read in product metadata
	        	Metadata met = readMetadata( new File(stagingDir, xmlmetFile) );
	        	// retrieve product id
	        	String id = SolrUtils.queryProduct(dataset, version, filename);
	          	LOG.info("Updating product name: "+filename+" id: "+id+" with metadata from file: "+xmlmetFile);
	          	
	          	if (id!=null) {
	          		// populate the map with the metadata to update
	          		updateMetadataMap.put(id, met);
	          	}
	          	
        	}
        		
        }    
        
        // send all updates at once
        if (updateMetadataMap.size()>0) {
        	String solrXmlDocument = SolrUtils.buildSolrXmlDocument(updateMetadataMap);
        	SolrUtils.postSolrXml(solrXmlDocument);
        }
		
	}
	
	/**
	 * Method to instruct the File Manager to reload its policy configuration.
	 * @throws Exception
	 */
	public static void reload() throws Exception {
		
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(FILEMANAGER_URL));
		
		boolean status = client.refreshConfigAndPolicy();

		// FIXME: sleep 5 seconds
		Thread.sleep(5000); 

		LOG.info("File Manager reoloaded, status="+status);
				
	}
	
	/** 
	 * Utility function to determine the latest version of an archived dataset.
	 * If not found, the latest version is set to 0.
	 */
	public static int findLatestDatasetVersion(final String datasetName) {
		
		File datasetDir = FileManagerUtils.getDatasetArchiveDir(datasetName);
		
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
    
        return readMetadata(datasetMetadataFile);
        
	}
	
	/**
	 * Utility method to read metadata from a file.
	 * 
	 * @param metadataFilepath
	 * @return
	 */
    public static Metadata readMetadata(final File metadataFilepath) {
		
		// read file metadata
        Metadata metadata = new Metadata(); // empty metadata container
        
        if (metadataFilepath.exists()) {
        	LOG.info("Reading metadata from file: "+metadataFilepath.getAbsolutePath());
        	
        	try {
        		 SerializableMetadata sm = new SerializableMetadata("UTF-8", false);
        		 sm.loadMetadataFromXmlStream(metadataFilepath.toURI().toURL().openStream());
        		 metadata = sm.getMetadata();
     			 for (String key : metadata.getAllKeys()) {
    				for (String val : metadata.getAllMetadata(key)) {
    					LOG.fine("\t==> Read metadata key=["+key+"] value=["+val+"]");
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
	 * Retrieves the directory where an uploaded dataset will be archived.
	 * 
	 * @param datasetName
	 * @return
	 */
	private static File getDatasetArchiveDir(final String datasetName) {
		
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/" + Constants.WORKFLOW_LABCAS_UPOLOAD;
		File datasetDir = new File(archiveDir, datasetName); 
		return datasetDir;
		
	}
	
	private static File getDatasetStagingDir(final String datasetName) {
		
		return new File(System.getenv(Constants.ENV_LABCAS_STAGING), datasetName);
		
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
	private static final void makeProductTypeElementMapXmlFile(File filepath, String productType) throws Exception {
		
		// XML document
		Document xmlDocument = XmlUtils.newXmlDocument();
		
		// <cas:producttypemap xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":producttypemap");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // <type id="urn:edrn:Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set" parent="urn:edrn:LabcasFile" />
        Element typeElement = xmlDocument.createElement("type");
        typeElement.setAttribute("id", "urn:edrn:"+productType);
        typeElement.setAttribute("parent", "urn:edrn:LabcasFile");
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
	public static final void makeProductTypesXmlFile(File filepath, String productTypeName, String description, Metadata metadata) throws Exception {
		
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
        
        // <repository path="file://[LABCAS_ARCHIVE]/labcas-upload"/>
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
        Element extractor1Element = xmlDocument.createElement("extractor");
        extractor1Element.setAttribute("class", CoreMetExtractor.class.getCanonicalName());
        metExtractorsElement.appendChild(extractor1Element);

        // <configuration>
        Element configuration1Element = xmlDocument.createElement("configuration");
        extractor1Element.appendChild(configuration1Element);

        // <property name="nsAware" value="true"/>
        Element property1Element = xmlDocument.createElement("property");
        property1Element.setAttribute("name", "nsAware");
        property1Element.setAttribute("value", "true");
        configuration1Element.appendChild(property1Element);
        
        // <property name="elements" value="ProductReceivedTime,ProductName,ProductId,ProductType"/>
        Element property2Element = xmlDocument.createElement("property");
        property2Element.setAttribute("name", "elements");
        property2Element.setAttribute("value", ELEMENT_LIST);
        configuration1Element.appendChild(property2Element);

        // <property name="elementNs" value="CAS"/>
        Element property3Element = xmlDocument.createElement("property");
        property3Element.setAttribute("name", "elementNs");
        property3Element.setAttribute("value", "CAS");
        configuration1Element.appendChild(property3Element);

        // <extractor class="org.apache.oodt.cas.filemgr.metadata.extractors.examples.MimeTypeExtractor">
        Element extractor2Element = xmlDocument.createElement("extractor");
        extractor2Element.setAttribute("class", MimeTypeExtractor.class.getCanonicalName());
        metExtractorsElement.appendChild(extractor2Element);
        
        // <configuration>
        Element configuration2Element = xmlDocument.createElement("configuration");
        extractor2Element.appendChild(configuration2Element);
        
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
       
        // loop over all metadata keys, values
        for (String key : metadata.getAllKeys()) {
        	for (String val : metadata.getAllMetadata(key)) {
        	
	        	// <keyval>
	        	Element keyvalElement = xmlDocument.createElement("keyval");
	        	metadataElement.appendChild(keyvalElement);
	        	
	        	// <key>Dataset</key>
	        	Element keyElement = xmlDocument.createElement("key");
	        	keyElement.insertBefore(xmlDocument.createTextNode(key), keyElement.getLastChild());
	        	keyvalElement.appendChild(keyElement);
	        	
	        	// <val>[Dataset]</val>
	        	Element valElement = xmlDocument.createElement("val");
	        	valElement.insertBefore(xmlDocument.createTextNode(val), valElement.getLastChild());
	        	keyvalElement.appendChild(valElement);  
	        	
	        }
        	
        }

        // write out the file
        XmlUtils.xmlToFile(xmlDocument, filepath);
		
	}

}
