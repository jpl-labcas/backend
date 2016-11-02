package gov.nasa.jpl.edrn.labcas.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.oodt.cas.filemgr.repository.XMLRepositoryManager;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.generators.LabcasProductIdGenerator;

/**
 * Class containing general utilities to query/update the Solr index.
 * 
 * @author luca
 *
 */
public class SolrUtils {
		
	private final static Logger LOG = Logger.getLogger(SolrUtils.class.getName());
	
	// default value for SOLR URL
	private static String SOLR_URL = "http://localhost:8983/solr";
	//private static String SOLR_URL = "http://localhost:8080/solr/oodt-fm";
	
	// list of OODT fields that are NOT transferred to the public Solr index
	private static Set<String> IGNORED_FIELDS = new HashSet<String>(
			Arrays.asList("WorkflowManagerUrl", "TaskId", "WorkflowInstId", "JobId",
				          "WorkflowId", "WorkflowName", "ProcessingNode"));

	private final static String SOLR_CORE_COLLECTIONS = "collections";
	private final static String SOLR_CORE_DATASETS = "datasets";
	private final static String SOLR_CORE_FILES = "files";
	
	// IMPORTANT: must re-use the same SolrServer instance across all requests to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861 
	// this method instantiates the shared instances of SolrServer (one per core)
	private static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
	static {
		try {
			solrServers.put(SOLR_CORE_COLLECTIONS, new CommonsHttpSolrServer( SOLR_URL+"/"+SOLR_CORE_COLLECTIONS) );
			solrServers.put(SOLR_CORE_DATASETS, new CommonsHttpSolrServer( SOLR_URL+"/"+SOLR_CORE_DATASETS) );
			solrServers.put(SOLR_CORE_FILES, new CommonsHttpSolrServer( SOLR_URL+"/"+SOLR_CORE_FILES) );
			
		} catch(MalformedURLException e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
		}
	}

	/**
	 * Utility method to query Solr for all products of a given dataset and version.
	 * 
	 * @param datasetName
	 * @param datasetVersion
	 * @return
	 */
	public static List<String> queryAllProducts(String datasetId, String datasetVersion) {
		
		List<String> ids = new ArrayList<String>();
		
		// build Solr query
        SolrQuery request = new SolrQuery();
        request.setQuery("*:*");
        request.addFilterQuery(Constants.METADATA_KEY_DATASET_ID+":"+datasetId,
        		               Constants.METADATA_KEY_DATASET_VERSION+":"+datasetVersion);
        request.setRows(Constants.MAX_SOLR_ROWS);
        LOG.fine("Executing Solr query: "+request.toString());
        
        // execute Solr query
        try {

	        QueryResponse response = solrServers.get(SOLR_CORE_FILES).query( request );
	        SolrDocumentList docs = response.getResults();
	        Iterator<SolrDocument> iter = docs.iterator();
	        while (iter.hasNext()) {
	            SolrDocument doc = iter.next();
	            //LOG.fine(doc.toString());
	            String id = (String) doc.getFieldValue("id"); 
	            LOG.fine("Retrieved Solr document id="+id);
	            ids.add(id);
	        }
	        
        } catch(Exception e) {
        	e.printStackTrace();
        	LOG.warning(e.getMessage()); // will return empty ids list
        }
		
		return ids;
		
	}
	
	/**
	 * Utility method to query Solr for a specific product belonging to a dataset and version.
	 * @param datasetId
	 * @param datasetVersion
	 * @param productName
	 * @return
	 */
	public static String queryProduct(String datasetId, String datasetVersion, String productName) {
		
		String id = null;
		
		// build Solr query
        SolrQuery request = new SolrQuery();
        request.setQuery("*:*");
        request.addFilterQuery(Constants.METADATA_KEY_DATASET_ID+":"+datasetId, 
        		               Constants.METADATA_KEY_DATASET_VERSION+":"+datasetVersion, 
        		               Constants.METADATA_KEY_PRODUCT_NAME+":"+productName);
        request.setRows(1);
        
        // execute Solr query
        try {

	        QueryResponse response = solrServers.get(SOLR_CORE_FILES).query( request );
	        SolrDocumentList docs = response.getResults();
	        Iterator<SolrDocument> iter = docs.iterator();
	        while (iter.hasNext()) {
	            SolrDocument doc = iter.next();
	            //LOG.fine(doc.toString());
	            id = (String) doc.getFieldValue("id"); 
	            LOG.info("Retrieved Solr document id="+id);
	        }
	        
        } catch(Exception e) {
        	e.printStackTrace();
        	LOG.warning(e.getMessage());
        }
        
        return id;
		
	}
	
	/**
	 * Method to publish an OODT ProductType (aka Collection) to Solr.
	 * @param productType
	 * @throws Exception
	 */
	public static void publishCollection(ProductType productType) throws Exception {
		
		FileManagerUtils.printMetadata(productType.getTypeMetadata());
		SolrInputDocument doc = serializeCollection(productType);
		LOG.info("Publishing Solr collection:"+doc.toString());
		solrServers.get(SOLR_CORE_COLLECTIONS).add(doc);
		solrServers.get(SOLR_CORE_COLLECTIONS).commit(); // FIXME: only at the very end
		
	}
	
	/**
	 * Alternative method to publish collections into Solr
	 * starting from the product-types.xml file located in the policy/ directory.
	 * Note that a single XML file may contain more than one OODT proruct type.
	 * 
	 * @param productTypeFile
	 * @throws Exception
	 */
	public static void publishCollection(File productTypesXmlFile) throws Exception {
		
		// parse XML file using OODT utilities
		// must reference the policy directory, for example:
		// "file:///usr/local/labcas_home/workflows/nist/policy/"
		XMLRepositoryManager xmlRP = new XMLRepositoryManager(
				Arrays.asList(new String[]{"file://"+productTypesXmlFile.getParent()}));
		List<ProductType> productTypes = xmlRP.getProductTypes();

		// publish new product type(s) to Solr
		for (ProductType pt : productTypes) {
			SolrUtils.publishCollection(pt);
		}
		
	}
	
	/**
	 * Method to publish dataset-level metadata to Solr.
	 * 
	 * @param productMetadata
	 * @throws Exception
	 */
	public static void publishDataset(Metadata metadata) throws Exception {
		
		FileManagerUtils.printMetadata(metadata);
		SolrInputDocument doc = serializeDataset(metadata);
		LOG.info("Publishing Solr dataset:"+doc.toString());
		solrServers.get(SOLR_CORE_DATASETS).add(doc);
		solrServers.get(SOLR_CORE_DATASETS).commit(); // FIXME: only at the very end

	}
	
	/**
	 * Method to publish an OODT product (aka file) to Solr.
	 * 
	 * @param metadata
	 * @throws Exception
	 */
	public static void publishFile(Metadata metadata) throws Exception {
		
		// FileManagerUtils.printMetadata(productMetadata);
		
		SolrInputDocument doc = serializeFile(metadata);
		LOG.info("Publishing product id="+doc.getFieldValue("id"));
		solrServers.get(SOLR_CORE_FILES).add(doc);
		solrServers.get(SOLR_CORE_FILES).commit(); // FIXME: only at the very end
		
	}
	
	/**
	 * Method that transforms an OODT ProductType into a Solr collection input document.
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	private static SolrInputDocument serializeCollection(ProductType productType) throws Exception {
		
		SolrInputDocument doc = new SolrInputDocument();
		
		doc.setField("id", productType.getProductTypeId());
		doc.setField("CollectionName", productType.getName());
		doc.setField("CollectionDescription", productType.getDescription());
		
		// serialize all metadata as-is
		// generally multi-valued fields
		Metadata metadata = productType.getTypeMetadata(); 
		for (String key : metadata.getAllKeys()) {
			for (String value : metadata.getAllMetadata(key)) {
				doc.addField(key, value);
			}
		}
		
		return doc;
	}
	
	/**
	 * Method that transforms OODT dataset level metadata into a Solr dataset input document.
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	private static SolrInputDocument serializeDataset(Metadata metadata) throws Exception {
		
		SolrInputDocument doc = new SolrInputDocument();
		
		String datasetId = metadata.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE) 
				         + "." 
				         + metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID);
		doc.setField("id", datasetId);
		
		// serialize all metadata
		for (String key : metadata.getAllKeys()) {
			
			// ignore OODT book-keeping fields
			if (IGNORED_FIELDS.contains(key))  {
				// do nothing
				
			// harvest Labcas core dataset attributes
			} else if (key.equals("ProductType")) {
				doc.setField("CollectionName", metadata.getMetadata(key));

			} else if (key.equals("DatasetId")) {
				// ignore, id already built

			} else if (key.equals("DatasetVersion")) {
				doc.setField("DatasetVersion", metadata.getMetadata(key));
				
			// transfer all other fields as-is
			// generally multi-valued
			} else {
				for (String value : metadata.getAllMetadata(key)) {
					doc.addField(key, value);
				}
			}

		}
		
		return doc;
		
	}
	
	/**
	 * Method that transforms OODT product level metadata into a Solr file input document.
	 * 
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	private static SolrInputDocument serializeFile(Metadata metadata) throws Exception {
		
		SolrInputDocument doc = new SolrInputDocument();
		
		// document unique identifier
		String productId = LabcasProductIdGenerator.generateId(metadata.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE), 
				                                               metadata.getMetadata(Constants.METADATA_KEY_DATASET_ID),
                                                               metadata.getMetadata(Constants.METADATA_KEY_PRODUCT_NAME) );
		doc.setField("id", productId);
		
		// serialize all metadata
		for (String key : metadata.getAllKeys()) {
			
			// ignore OODT book-keeping fields
			if (IGNORED_FIELDS.contains(key))  {
				// do nothing
				
			// harvest Labcas core file attributes
			} else if (key.equals("ProductType")) {
				doc.setField("CollectionName", metadata.getMetadata(key));
								
			} else if (key.equals("DatasetId")) {
				doc.setField("DatasetId", metadata.getMetadata(key));

			} else if (key.equals("DatasetVersion")) {
				doc.setField("DatasetVersion", metadata.getMetadata(key));
				
			} else if (key.equals("ProductName")) {
				// ignore, same as Filename
				
			} else if (key.equals("FileLocation")) {
				doc.setField("FileLocation", metadata.getMetadata(key));

			} else if (key.equals("Filename")) {
				doc.setField("FileName", metadata.getMetadata(key));
				
			} else if (key.equals("FileSize")) {
				doc.setField("FileSize", metadata.getMetadata(key));

			// transfer all other fields as-is
			// generally multi-valued
			} else {
				for (String value : metadata.getAllMetadata(key)) {
					doc.addField(key, value);
				}
			}
		}
		
		return doc;
		
	}
	
	/**
	 * Utility method to build a Solr XML update document 
	 * for all given records and all metadata fields.
	 * @param ids
	 * @param datasetMetadata
	 * @return
	 */
	@Deprecated
	public static String buildSolrXmlDocument(HashMap<String, Metadata> updateMetadataMap) throws Exception {
		
        // create Solr/XML update document
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document xmlDocument = builder.newDocument();
        
        // <add>
        Element addElement = xmlDocument.createElement("add");
        xmlDocument.appendChild(addElement);
        
        // loop over all records that must be updated
        for (String id : updateMetadataMap.keySet()) {
        
	        // <doc>
	        Element docElement = xmlDocument.createElement("doc");
	        addElement.appendChild(docElement);
	        
	        // <field name="id">38b6e7e6-3a9b-4565-9d57-37e8104b4fde</field>
	        Element fieldElement = xmlDocument.createElement("field");
	        fieldElement.setAttribute("name", "id");
	        fieldElement.insertBefore(xmlDocument.createTextNode(id), fieldElement.getLastChild());
	        docElement.appendChild(fieldElement);
	        
	         // to add one or more values:
	         // <field name="Institution" update="set">Darthmouth</field>
	         // <field name="Institution" update="set">Children Hospital</field>
	         // to remove a key:
	         // <field name="Institution" update="set" null="true"/>
	        Metadata metadata = updateMetadataMap.get(id);
			 for (String key : metadata.getAllKeys()) {
				for (String val : metadata.getAllMetadata(key)) {
					LOG.info("\t==> XML: Updating dataset metadata key=["+key+"] value=["+val+"]");
					
					Element metFieldElement = xmlDocument.createElement("field");
					metFieldElement.setAttribute("name", key);
					metFieldElement.setAttribute("update", "set");
					if (StringUtils.hasText(val)) {
						// add this value to that key
						metFieldElement.insertBefore(xmlDocument.createTextNode(val), metFieldElement.getLastChild());
					} else {
						// remove all values for that key
						metFieldElement.setAttribute("null", "true");
					}
			        docElement.appendChild(metFieldElement);
					
				}
			 }
        
        } // loop over record ids
			 
        String xmlString = XmlUtils.xmlToString(xmlDocument);
        LOG.info(xmlString);
        return xmlString;

	}
	
	/**
	 * Utility method to POST an XML document to Solr
	 * @param solrXmlDocument
	 */
	@Deprecated
	public static void postSolrXml(String solrXmlDocument) {
		
	    //String strURL = "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm/update?commit=true";
	    String solrUpdateUrl = SOLR_URL + "/update?commit=true";
	
	    HttpClient client = new DefaultHttpClient();
	    HttpPost post = new HttpPost(solrUpdateUrl);
	    
	    try {
		    HttpEntity entity = new ByteArrayEntity(solrXmlDocument.getBytes("UTF-8"));
		    post.setEntity(entity);
		    post.setHeader("Content-Type", "application/xml");
		    HttpResponse response = client.execute(post);
		    String result = EntityUtils.toString(response.getEntity());
		    LOG.info("POST result="+result);
	   
	    } catch(Exception e) {
	    	LOG.warning(e.getMessage());
	    	
	    } finally {   
		    // must release connection
		    post.releaseConnection();
	    }
	    
	}
	
	public static void main(String[] args) throws Exception {
		
		// FIXME: insert help
		String productTypesXmlFilePath = args[0]; 
		System.out.println("Parsing file: "+productTypesXmlFilePath);
		File productTypesXmlFile = new File(productTypesXmlFilePath);
		if (!productTypesXmlFile.exists()) {
			throw new Exception("File "+productTypesXmlFilePath+" not found");
		}
		
		// publish all product types in file
		SolrUtils.publishCollection(productTypesXmlFile);
		
	}

}
