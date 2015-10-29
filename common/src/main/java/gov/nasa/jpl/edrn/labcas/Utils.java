package gov.nasa.jpl.edrn.labcas;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor;
import org.apache.oodt.cas.filemgr.metadata.extractors.examples.MimeTypeExtractor;
import org.apache.oodt.cas.filemgr.structs.ExtractorSpec;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Utils {
		
	private static final Logger LOG = Logger.getLogger(Utils.class.getName());
	
	// parameters that enter into product type definition
	private static final String REPOSITORY = "file://[LABCAS_ARCHIVE]/labcas-upload";
	private static final String VERSIONER = "gov.nasa.jpl.edrn.labcas.versioning.LabcasProductVersioner";
	private final static String ELEMENT_LIST = "ProductReceivedTime,ProductName,ProductId,ProductType";

	
	/** 
	 * Utility function to determine the latest version of an archived dataset.
	 * If not found, the latest version is set to 0.
	 */
	public static int findLatestDatasetVersion(final String datasetName) {
		
		File datasetDir = getDatasetDir(datasetName);
		
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
	 * Retrieves the directory where an uploaded dataset will be archived.
	 * 
	 * @param datasetName
	 * @return
	 */
	public static File getDatasetDir(final String datasetName) {
		
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/" + Constants.WORKFLOW_LABCAS_UPOLOAD;
		File datasetDir = new File(archiveDir, datasetName); 
		return datasetDir;
		
	}
	
	/**
	 * Constructs the product type name from a dataset identifier.
	 * @param dataset
	 * @return
	 */
	public static String getProductTypeName(String dataset) {
		
		String productType = WordUtils.capitalize(dataset).replaceAll("\\s+", "_");
		return productType;
		
	}
	
	/**
	 * Utility method that reads the additional dataset metadata 
	 * from the file DatasetMetadata.xml located in the dataset staging directory.
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException
	 */
	public static Metadata readDatasetMetadata(final String datasetName) {
		
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
	 * Factory method to create a new XML document object.
	 * @return
	 */
	public static final Document newXmlDocument() throws Exception {
		
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document xmlDocument = builder.newDocument();

        return xmlDocument;
	}

	/**
	 * Method to transform an XML document into a pretty-formatted string.
	 * @param xml
	 * @return
	 * @throws Exception
	 */
	public static final String xmlToString(Document xml) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		return out.toString();
	}
	
	/**
	 * Method to write an XML document to a file, will override existing file.
	 * @param file
	 * @throws Exception
	 */
	public static final void xmlToFile(Document xmldoc, File file) throws Exception {
		
		// pretty formatting
		String xmlstring = xmlToString(xmldoc);
		System.out.println(xmlstring);
		
		// write out
		FileUtils.writeStringToFile(file, xmlstring);
		
	}
	
	/**
	 * Utility method to create the file "elements.xml".
	 * @param filepath
	 * @throws Exception
	 */
	public static final void makeElementsXmlFile(File filepath) throws Exception {

		// XML document
		Document xmlDocument = Utils.newXmlDocument();
        
        // <cas:elements xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":elements");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // write out the file
        Utils.xmlToFile(xmlDocument, filepath);

	}
	
	/**
	 * Utility method to create the file "product-type-element-map.xml".
	 * @param filepath
	 * @param productType
	 * @throws Exception
	 */
	public static final void makeProductTypeElementMapXmlFile(File filepath, String productType) throws Exception {
		
		// XML document
		Document xmlDocument = Utils.newXmlDocument();
		
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
        Utils.xmlToFile(xmlDocument, filepath);
		
	}
	
	
	/**
	 * Utility method to create the file "product-types.xml".
	 * @param filepath
	 * @param productType
	 * @param description
	 * @param metadata
	 * @throws Exception
	 */
	public static final void makeProductTypesXmlFile(File filepath, String productType, String description, Metadata metadata) throws Exception {
		
		// XML document
		Document xmlDocument = Utils.newXmlDocument();
		
		// <cas:producttypes xmlns:cas="http://oodt.apache.org/components/cas">
        Element rootElement = xmlDocument.createElement(Constants.PREFIX+":producttypes");
        rootElement.setAttribute("xmlns:"+Constants.PREFIX, Constants.NS);
        xmlDocument.appendChild(rootElement);
        
        // <type id="urn:oodt:Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set" name="Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set">
        Element typeElement = xmlDocument.createElement("type");
        typeElement.setAttribute("id", "urn:edrn:"+productType);
        typeElement.setAttribute("name", productType);
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
        Utils.xmlToFile(xmlDocument, filepath);
		
	}
	
	/**
	 * Method to retrieve a ProductType from the File Manager XML/RPC interface.
	 * 
	 * @param productTypeName
	 */
	/**
	public final static ProductType getProductType(String productTypeName) throws Exception {
		
		// instantiate XML/RPC client to File Manager
		String fmURL = System.getenv(Constants.ENV_FILEMGR_URL);
		if (fmURL==null) {
			fmURL = "http://localhost:9000/";
		}
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(fmURL));
		
		ProductType productType = client.getProductTypeByName(productTypeName);
		
		return productType;
		
	}*/
	
	/**
	 * Method that adds or updates a ProductType through the File Manager XML/RPC interface.
	 */
	public final static void addProductType(String productTypeName, String datasetDescription, Metadata metadata) throws Exception {
		
		// instantiate XML/RPC client to File Manager
		String fmURL = System.getenv(Constants.ENV_FILEMGR_URL);
		if (fmURL==null) {
			fmURL = "http://localhost:9000/";
		}
		XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(fmURL));
		
		// create a product type
		String repository = REPOSITORY.replace("[LABCAS_ARCHIVE]", System.getenv(Constants.ENV_LABCAS_ARCHIVE));
		String versioner = VERSIONER;
		String id = "urn:edrn:"+productTypeName;
		ProductType productType = new ProductType(id, productTypeName, datasetDescription, repository, versioner);
		
		// add metadata extractors
		List<ExtractorSpec> extractors = new ArrayList<ExtractorSpec>();
		
		// org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor
		ExtractorSpec coreMetExtractor = new ExtractorSpec();
		coreMetExtractor.setClassName(CoreMetExtractor.class.getCanonicalName());
		Properties config = new Properties();
		config.setProperty("nsAware", "true");
		config.setProperty("elements", ELEMENT_LIST);
		config.setProperty("elementNs", "CAS");
		coreMetExtractor.setConfiguration(config);
		extractors.add(coreMetExtractor);
		
		// org.apache.oodt.cas.filemgr.metadata.extractors.examples.MimeTypeExtractor
		ExtractorSpec mimeTypeExtractor = new ExtractorSpec();
		mimeTypeExtractor.setClassName(MimeTypeExtractor.class.getCanonicalName());
		extractors.add(mimeTypeExtractor);
		
		productType.setExtractors(extractors);
		
		// add dataset metadata
		productType.setTypeMetadata(metadata);
		
		// add this product type to the File Manager
		String productTypeId = client.addProductType(productType);
		System.out.println("Inserted product type: "+productTypeId);
		
		
	}

}
