package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import gov.nasa.jpl.labcas.data_access_api.utils.HttpClient;
import gov.nasa.jpl.labcas.data_access_api.utils.Serializer;
import gov.nasa.jpl.labcas.data_access_api.utils.UpdateDocumentParser;
import gov.nasa.jpl.labcas.data_access_api.utils.UrlUtils;
import gov.nasa.jpl.labcas.data_access_api.utils.XmlParser;

/**
 * Service implementation to update the LabCAS metadata
 * 
 * @author Luca Cinquini
 *
 */
@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class MetadataServiceImpl extends SolrProxy implements MetadataService {
	
	private final static String XPATH1 = "/response/result";
	private final static String XPATH2 = "/response/result/doc/str[@name='id']";
	private final static int LIMIT = 100;
	
	private XPath xPath1;
	private XPath xPath2;

	private final static Logger LOG = Logger.getLogger(MetadataServiceImpl.class.getName());

	public MetadataServiceImpl() throws JDOMException {
		
		super();
		xPath1 = XPath.newInstance(XPATH1);
		xPath2 = XPath.newInstance(XPATH2);

	}

	@Override
	@GET
	@Path("/updateById")
	public Response updateById(@Context HttpServletRequest httpRequest, 
			@Context ContainerRequestContext requestContext,
			@Context HttpHeaders headers,
			@QueryParam("core") List<String> cores, @QueryParam("action") String action, @QueryParam("id") String id,
			@QueryParam("field") String field, @QueryParam("value") List<String> values) {
		
		LOG.info("/updateById request: cores="+cores+" action="+action+" id="+id+" field="+field+" values="+values);
		
	    	// execute update across all cores
	    	int numRecordsUpdated = 0;
	    	try {
	    		
	    		// build update map of a single (query criteria, metadata content) pair 
		    	HashMap<String, Map<String,List<String>>> doc = new HashMap<String, Map<String, List<String>>>();
		    	Map<String,List<String>> metadata = new HashMap<String,List<String>>();
		    	metadata.put(field, values);
		    	String query = "id:"+UrlUtils.encode(id);
		    	// add additional access control constraint
		    	String accessControlQuery = getAccessControlQueryStringValue(requestContext);
		    	LOG.info("UpdateById request: access control constraint="+accessControlQuery);
		    	if (!accessControlQuery.isEmpty()) {
		    		query += "&" + accessControlQuery;
		    	}
		    	doc.put(query, metadata);
		    	
		    	// execute metadata updates for each core separately
		    	for (String core : cores) {
		    		numRecordsUpdated += this._update(getBaseUrl(core), action, doc);
		    	}
		    	
	    	} catch(Exception e) {
	    		e.printStackTrace();
	    		LOG.warning(e.getMessage());
	    		return Response.status(500).entity(e.getMessage()).build();
	    	}
    			
    		String message = "Total number of records updated: "+numRecordsUpdated+" (across all cores).";
		return Response.status(200).entity( message ).build();
	}

	@Override
	@POST
	@Path("/update")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
	public Response update(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext, @Context HttpHeaders headers,
			String document) {
		
		LOG.info("/update request: " + document);
		String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
		LOG.info("/update content type: " + contentType);
				
		int numRecordsUpdated = 0;
		try {
			
			// parse input document
			UpdateDocumentParser parser = new UpdateDocumentParser(document, contentType);
			String action = parser.getAction();
			String[] cores = parser.getCores();
			HashMap<String, Map<String,List<String>>> doc = parser.getDoc();
			
			// execute metadata updates for each core separately
			for (String core : cores) {
				numRecordsUpdated += this._update(getBaseUrl(core.trim()), action, doc);
			}
		
		} catch(Exception e) {
	    		e.printStackTrace();
	    		LOG.warning(e.getMessage());
	    		return Response.status(500).entity(e.getMessage()).build();
		}
		
		String message = "Total number of records updated: "+numRecordsUpdated+" (across all cores).";
		return Response.status(200).entity( message ).build();

		
	}
	
	/**
	 * Method to change/add/remove metadata fields to/from existing records.
	 * @param solrCoreUrl: core specific Solr URL (example: "http://localhost:8983/solr/datasets")
	 * @param action: one of "set", "add", "remove"
	 * @param metadata: dictionary of queries to map of field name and values to be updated for all matching results
	 *                  example:
	 *                  { 
	 *                    'id:Boston_University_Lung_Tumor_Sequencing': 
     *          	         {
     *                     'LeadPI':['John Smith','Jane Bridge']
     *                    }
     *                  }
	 *
	 * @return: number of documents that matched the query (and therefore updated - across all queries combined)
	 */
	public int _update(String solrCoreUrl, String action, HashMap<String, Map<String,List<String>>> map) throws Exception {

		HttpClient httpClient = new HttpClient();
		XmlParser xmlParser = new XmlParser(false);
		
		LOG.info("Metadata update: url="+solrCoreUrl+" action="+action);
		
		// process each query separately
		int numRecordsUpdated = 0;
		for (String query : map.keySet()) {
		
			Map<String,List<String>> metadata = map.get(query);
			LOG.info("Processing query: "+query);
			String[] constraints = query.split("&");
			
	        // VERY IMPORTANT: MUST FIRST CREATE ALL THE UPDATE DOCUMENTS, 
	        // THEN SENDING THEM WITH A commit=True STATEMENT
	        // OTHERWISE PAGINATION OF RESULTS DOES NOT WORK
			List<String> xmlDocs = new ArrayList<String>();
			
			// query for ALL matching records
			int start = 0;
			int numFound = start + 1;
			while (start < numFound) {
								
				// build query URL - must be URL-encoded
				String selectUrl = solrCoreUrl + "/select?"
				            	     + "q="+UrlUtils.encode("*:*")
				             	 + "&fl=id"
				             	 + "&wt=xml"
				             	 + "&indent=true"
				             	 + "&start="+start
				             	 + "&rows="+LIMIT;
				for (String constraint : constraints) {
					// NOTE: split only at first occurrence of ':' to allow for values that contain the character ':'
					String[] kv = constraint.split(":", 2); 
					selectUrl += "&fq="+UrlUtils.encode(kv[0]+":"+kv[1]);
				}
				
				// execute HTTP query request
			    Response response = httpClient.doGet(selectUrl);
			    String _response = response.getEntity().toString();
			    LOG.info("HTTP response:" +_response);
			  
			    final Document xmlDoc = xmlParser.parseString(_response);
			    
			    // total number of results
	            // <result name="response" numFound="9" start="0" maxScore="1.0">
	            Element resultElement = (Element)xPath1.selectSingleNode(xmlDoc);
	            numFound = Integer.parseInt( resultElement.getAttributeValue("numFound") );
	            
	            // build the XML update document
	            /**
	             <?xml version="1.0" encoding="UTF-8" standalone="no"?>
				 <add>
				 	<doc>...</doc>
				 	<doc>...</doc>
				 	..............
				 </add>
	             */
	            Element _addElement = new Element("add");
	            List<Element> _docElements = _buildUpdateDocuments(xmlDoc, action, metadata);
	            _addElement.addContent(_docElements);
	            Document _jdoc = new Document(_addElement);
				xmlDocs.add( Serializer.JDOMtoString(_jdoc) );
				
				// increment counter for next request
				start += _docElements.size();
				
			} // loop over multiple HTTP query request/response
			
			// send all updates, commit each time
			String updateUrl = solrCoreUrl + "/update?commit=true";
			for (String xmlDoc : xmlDocs) {
				LOG.info("Solr update document:"+xmlDoc);
				httpClient.doPost(new URL(updateUrl), xmlDoc, true); // xml=true
			}
			
			numRecordsUpdated += numFound;
					
		} // loop over queries
		
		return numRecordsUpdated;
		
	}
	
    // Method to build an XML update document snippet
    //	<doc>
    //		<field name="id">Boston_University_Lung_Tumor_Sequencing</field>
    //		<field name="LeadPI" update="add">Jim Adams</field>
    //	</doc>
	private List<Element> _buildUpdateDocuments(Document xmlDoc, String action, Map<String,List<String>> metadata) throws Exception {
        
		List<Element> _docElements = new ArrayList<Element>();
		
        // loop over results across this response
        for (Object obj : xPath2.selectNodes(xmlDoc)) {
        	
	        	// will start from next record
	        	Element idElement = (Element)obj;
	        	String id = idElement.getText();
	        	        	
	        	Element _docElement = new Element("doc");
	        	Element _idElement = new Element("field");
	        	_idElement.setAttribute("name", "id");
	        	_idElement.setText(id);
	        	_docElement.addContent(_idElement);
	        	
	        	// loop over metadata keys to set/add/remove
	        	for (String key : metadata.keySet()) {
	        		List<String> values = metadata.get(key);
	        		
	        		if (values.size()>0) {
	        		
	            		// set all new values
	            		for (String value : values) {	
	            			Element _fieldElement = new Element("field");
	            			_fieldElement.setAttribute("name", key);
	            			_fieldElement.setAttribute("update", action.toLowerCase());
	            			_fieldElement.setText(value);
	            			_docElement.addContent(_fieldElement);
	            		}
	            		
	        		} else {
	        			
	        			// remove all values
	        			// <field name="xlink" update="set" null="true"/>
	        			Element _fieldElement = new Element("field");
	        			_fieldElement.setAttribute("name", key);
	        			_fieldElement.setAttribute("update", action.toLowerCase());
	        			_fieldElement.setAttribute("null", "true");
	        			_docElement.addContent(_fieldElement);
	
	        		}
	        		
	        	}
        	
	        	_docElements.add(_docElement);
         	
        } // loop over results within one HTTP response		
        
        return _docElements;

	}

}
