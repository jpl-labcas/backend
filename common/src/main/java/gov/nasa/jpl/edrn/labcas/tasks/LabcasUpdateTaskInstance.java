package gov.nasa.jpl.edrn.labcas.tasks;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

/**
 * Task used to update the metadata of an already published dataset.
 * 
 * @author luca
 *
 */
public class LabcasUpdateTaskInstance implements WorkflowTaskInstance {
	
	private static final Logger LOG = Logger.getLogger(LabcasUpdateTaskInstance.class.getName());
	
	// Solr connection parameters
	private String solrUrl = null; // "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm"
	private SolrServer solrServer = null;
	
	// IMPORTANT: must re-use the same SolrServer instance across all requests to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861 
	// this method instantiates the shared instance of SolrServer
	private synchronized void init(String solrUrl) {
		try {
			this.solrUrl = solrUrl;
			this.solrServer = new CommonsHttpSolrServer( this.solrUrl );
		} catch(MalformedURLException e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
		}
	}
	
	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {

		try {
			
			// one-time only: instantiate the shared Solr server instance
			if (this.solrServer==null) {
				this.init( config.getProperty("solr.url") );
			}
			
			// retrieve dataset name
			String datasetName = metadata.getMetadata(Constants.METADATA_KEY_DATASET);
			
			// determine latest dataset version
			String datasetVersion = Integer.toString( Utils.findLatestDatasetVersion( datasetName ) );
			LOG.info("Updating metadata for dataset: "+datasetName+" version:"+datasetVersion);
			
			// query Solr for all product ids matching the given dataset name and version
			List<String> ids = querySolr(datasetName, datasetVersion);
			
			// read dataset metadata
			Metadata datasetMetadata = Utils.readDatasetMetadata(datasetName);
			
			// create Solr XML update document
			String solrXmlDocument = buildSolrXmlDocument(ids, datasetMetadata);
			
			// send update request to Solr
			postSolrXml(solrXmlDocument);
		
		} catch(Exception e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
		}
		
	}
	
	/**
	 * Utility method to query Solr for all matching records and retrieve their ids
	 * 
	 * @param datasetName
	 * @param datasetVersion
	 * @return
	 */
	private List<String> querySolr(String datasetName, String datasetVersion) {
		
		List<String> ids = new ArrayList<String>();
		
		// build Solr query
        SolrQuery request = new SolrQuery();
        request.setQuery("*:*");
        request.addFilterQuery("Dataset:"+datasetName,"Version:"+datasetVersion);
        request.setRows(Constants.MAX_SOLR_ROWS);
        
        // execute Solr query
        try {

	        QueryResponse response = solrServer.query( request );
	        SolrDocumentList docs = response.getResults();
	        Iterator<SolrDocument> iter = docs.iterator();
	        while (iter.hasNext()) {
	            SolrDocument doc = iter.next();
	            //LOG.fine(doc.toString());
	            String id = (String) doc.getFieldValue("id"); 
	            LOG.info("Retrieved Solr document id="+id);
	            ids.add(id);
	        }
	        
        } catch(Exception e) {
        	e.printStackTrace();
        	LOG.warning(e.getMessage()); // will return empty ids list
        }
		
		return ids;
		
	}
	
	/**
	 * Utility method to build a Solr XML update document 
	 * for all given records and all metadata fields.
	 * @param ids
	 * @param datasetMetadata
	 * @return
	 */
	private String buildSolrXmlDocument(List<String> ids, Metadata datasetMetadata) throws Exception {
		
        // create Solr/XML update document
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document xmlDocument = builder.newDocument();
        
        // <add>
        Element addElement = xmlDocument.createElement("add");
        xmlDocument.appendChild(addElement);
        
        // loop over all records that must be updated
        for (String id : ids) {
        
	        // <doc>
	        Element docElement = xmlDocument.createElement("doc");
	        addElement.appendChild(docElement);
	        
	        // <field name="id">38b6e7e6-3a9b-4565-9d57-37e8104b4fde</field>
	        Element fieldElement = xmlDocument.createElement("field");
	        fieldElement.setAttribute("name", "id");
	        fieldElement.insertBefore(xmlDocument.createTextNode(id), fieldElement.getLastChild());
	        docElement.appendChild(fieldElement);
	        
	        // <field name="Institution" update="set">Darthmouth</field>
			 for (String key : datasetMetadata.getAllKeys()) {
				for (String val : datasetMetadata.getAllMetadata(key)) {
					LOG.info("\t==> XML: Updating dataset metadata key=["+key+"] value=["+val+"]");
					
					Element metFieldElement = xmlDocument.createElement("field");
					metFieldElement.setAttribute("name", key);
					metFieldElement.setAttribute("update", "set");
					metFieldElement.insertBefore(xmlDocument.createTextNode(val), metFieldElement.getLastChild());
			        docElement.appendChild(metFieldElement);
					
				}
			 }
        
        } // loop over record ids
			 
        String xmlString = Utils.xmlToString(xmlDocument);
        LOG.info(xmlString);
        return xmlString;

	}
	
	/**
	 * Utility method to POST an XML document to Solr
	 * @param solrXmlDocument
	 */
	private void postSolrXml(String solrXmlDocument) {
		
	    //String strURL = "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm/update?commit=true";
	    String solrUpdateUrl = this.solrUrl + "/update?commit=true";
	
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

}
