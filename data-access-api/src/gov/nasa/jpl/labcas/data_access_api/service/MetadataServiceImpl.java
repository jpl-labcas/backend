package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.net.URLEncoder;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import gov.nasa.jpl.labcas.data_access_api.utils.HttpClient;
import gov.nasa.jpl.labcas.data_access_api.utils.Serializer;
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
	public Response updateById(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
			@QueryParam("core") String core, @QueryParam("action") String action, @QueryParam("id") String id,
			@QueryParam("field") String field, @QueryParam("value") List<String> values) {
		
		LOG.info("UpdateById request: core="+core+" action="+action+" id="+id+" field="+field+" values="+values);
		
	    	// execute update
	    	int numRecordsUpdated = 0;
	    	try {
	    		
	    		// build update map of a single (query criteria, metadata content) pair 
		    	HashMap<String, Map<String,List<String>>> doc = new HashMap<String, Map<String, List<String>>>();
		    	Map<String,List<String>> metadata = new HashMap<String,List<String>>();
		    	metadata.put(field, values);
		    	String query = "id:"+id;
		    	// add additional access control constraint
		    	String accessControlQuery = getAccessControlQueryStringValue(requestContext);
		    	LOG.info("UpdateById request: access control constraint="+accessControlQuery);
		    	if (!accessControlQuery.isEmpty()) {
		    		query += "&" + accessControlQuery;
		    	}
		    	doc.put(query, metadata);
		    	
		    	// invoke metadata update service
		    	numRecordsUpdated = this._update(getBaseUrl(core), action, doc);
		    	
	    	} catch(Exception e) {
	    		e.printStackTrace();
	    		LOG.warning(e.getMessage());
	    		return Response.status(500).entity(e.getMessage()).build();
	    	}
    			
    		String message = "Number of records updated: "+numRecordsUpdated+" .";
		return Response.status(200).entity( message ).build();
	}

	@Override
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_XML)
	public Response update(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext, String content) {
		// TODO Auto-generated method stub
		LOG.info(content);
		return Response.status(200).entity("OK").build();
	}
	
	/**
	 * Method to change/add/remove metadata fields to/from existing records.
	 * @param url: base Solr URL (example: "http://localhost:8983/solr")
	 * @param core: Solr core (example: "datasets")
	 * @param action: one of "set", "add", "remove"
	 * @param metadata: dictionary of queries to map of field name and values to be updated for all matching results
	 *                  example:
	 *                  { 
	 *                    'id:test.test.v1.testData.nc|esgf-dev.jpl.nasa.gov': 
     *          	         {
     *                     'xlink':['http://esg-datanode.jpl.nasa.gov/.../zosTechNote_AVISO_L4_199210-201012.pdf|AVISO Sea Surface Height Technical Note|summary']
     *                    }
     *                  }
	 *
	 * @return: number of documents that matched the query (and therefore updated - across all queries combined)
	 */
	public int _update(String solrCoreUrl, String action, HashMap<String, Map<String,List<String>>> doc) throws Exception {

		HttpClient httpClient = new HttpClient();
		XmlParser xmlParser = new XmlParser(false);
		
		LOG.info("Metadata update: url="+solrCoreUrl+" action="+action);
		
		// process each query separately
		int numRecordsUpdated = 0;
		for (String query : doc.keySet()) {
		
			Map<String,List<String>> metadata = doc.get(query);
			LOG.info("Processing query: "+query);
			String[] constraints = query.split("&");
			
	        // VERY IMPORTANT: MUST FIRST CREATE ALL THE UPDATE DOCUMENTS, 
	        // THEN SENDING THEM WITH A commit=True STATEMENT
	        // OTHERWISE PAGINATION OF RESULTS DOES NOT WORK
			List<String> xmlDocs = new ArrayList<String>();
			
			// query ALL matching records
			int start = 0;
			int numFound = start + 1;
			while (start < numFound) {
								
				// build query URL
				String selectUrl = solrCoreUrl + "/select?"
				            	 + "q="+URLEncoder.encode("*:*","UTF-8")
				            	 + "&fl=id"
				            	 + "&wt=xml"
				            	 + "&indent=true"
				            	 + "&start="+start
				            	 + "&rows="+LIMIT;
				for (String constraint : constraints) {
					// NOTE: split only at first occurrence of ':' to allow for values that contain the character ':'
					String[] kv = constraint.split(":", 2); 
					selectUrl += "&fq="+kv[0]+":"+URLEncoder.encode(kv[1],"UTF-8"); // must URL-encode the values
				}
				
				// execute HTTP query request
				LOG.info("HTTP request: "+selectUrl);
			    String response = httpClient.doGet(new URL(selectUrl));
			    LOG.info("HTTP respose:" +response);
			    
			    // parse HTTP query response
			    final Document xmlDoc = xmlParser.parseString(response);
			    
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
				httpClient.doPost(new URL(updateUrl), xmlDoc, true); // xml=true
			}
			
			numRecordsUpdated += numFound;
					
		} // loop over queries
		
		return numRecordsUpdated;
		
	}
	
    // Method to build an XML update document snippet
    //	<doc>
    //		<field name="id">cmip5.output1.CSIRO-BOM.ACCESS1-3.historical.mon.ocean.Omon.r1i1p1.v2|aims3.llnl.gov</field>
    //		<field name="xlink" update="add">ccc</field>
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
