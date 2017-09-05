package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;




@Path("/")
@Produces(MediaType.TEXT_PLAIN) 
public class DownloadServiceImpl implements DownloadService {
	
	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());
	
	// default base Solr URL if $FILEMGR_URL is not set
	private static String SOLR_URL = "http://localhost:8983/solr"; 
	private final static String SOLR_CORE_COLLECTIONS = "collections";
	private final static String SOLR_CORE_DATASETS = "datasets";
	private final static String SOLR_CORE_FILES = "files";
	private final static String SOLR_FIELD_FILE_DOWNLOAD_ID = "FileDownloadId";
	
	/**
	 * Configuration file located in user home directory
	 */
	private final static String LABCAS_PROPERTIES = "/labcas.properties";
	private final static String DATA_ACCESS_API_BASE_URL_PROPERTY = "dataAccessApiBaseUrl";

	
	// IMPORTANT: must re-use the same SolrServer instance across all requests to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861 
	// this method instantiates the shared instances of SolrServer (one per core)
	private static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
	static {
		try {
			
			if (System.getenv("FILEMGR_URL")!=null) {
				SOLR_URL = System.getenv("FILEMGR_URL").replaceAll("9000", "8983")+"/solr";
			}
			// FIXME
			SOLR_URL = "https://mcl-labcas.jpl.nasa.gov/solr";
			LOG.info("Using base SOLR_URL="+SOLR_URL);
			
			solrServers.put(SOLR_CORE_COLLECTIONS, new CommonsHttpSolrServer(SOLR_URL+"/"+SOLR_CORE_COLLECTIONS) );
			solrServers.put(SOLR_CORE_DATASETS, new CommonsHttpSolrServer(SOLR_URL+"/"+SOLR_CORE_DATASETS) );
			solrServers.put(SOLR_CORE_FILES, new CommonsHttpSolrServer(SOLR_URL+"/"+SOLR_CORE_FILES) );
			
		} catch(MalformedURLException e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
		}
	}
	
	private String dataAccessApiBaseUrl = null;
	
	/** Constructor reads base download URL from configuration properties. */
	public DownloadServiceImpl() {
		
    	LOG.info("Initializing DownloadServiceImpl");
    	
    	try {
			InputStream input = new FileInputStream(System.getProperty("user.home") + LABCAS_PROPERTIES);
			Properties properties = new Properties();
			properties.load(input);
            this.dataAccessApiBaseUrl = properties.getProperty(DATA_ACCESS_API_BASE_URL_PROPERTY);
            LOG.info("Using dataAccessApiBaseUrl="+this.dataAccessApiBaseUrl );

    	} catch(IOException e) {
    		LOG.warning("Eroor reading property file: " + LABCAS_PROPERTIES);
    	}

	}

	@Override
	@GET
	@Path("/files/select")
	public Response downloadFiles(@Context HttpServletRequest httpRequest,
			                      @QueryParam("q") String q, 
			                      @QueryParam("fq") List<String> fq, 
			                      @QueryParam("start") int start, 
			                      @QueryParam("rows")int rows, 
			                      @QueryParam("sort") String sort) {
		
		LOG.info("HTTP request URL="+httpRequest.getRequestURL());
		LOG.info("HTTP request query string="+httpRequest.getQueryString());
		LOG.info("HTTP request parameters q="+q+" fq="+fq+" start="+start+" rows="+rows+" sort="+sort);
		
		// build Solr query
		SolrQuery request = new SolrQuery( httpRequest.getQueryString() );
		if (q != null && !q.isEmpty()) { request.setQuery(q); }
		if (fq != null && fq.size()>0) { request.setFilterQueries(fq.toArray(new String[fq.size()])); }
		if (start>0) { request.setStart(start); }
		if (rows>0) { request.setRows(rows); }
		if (sort != null && !sort.isEmpty()) { 
			String[] parts = sort.split("\\s+");
			request.setSortField(parts[0], ORDER.valueOf(parts[1]));
		}
        String[] fields = new String[]{ SOLR_FIELD_FILE_DOWNLOAD_ID }; 
        request.setFields(fields);
		
        // execute Solr query, build result document
        String result = "";
        try {

	        QueryResponse response = solrServers.get(SOLR_CORE_FILES).query( request );
	        SolrDocumentList docs = response.getResults();
	        Iterator<SolrDocument> iter = docs.iterator();
	        while (iter.hasNext()) {
	            SolrDocument doc = iter.next();
	            LOG.fine(doc.toString());
	            String fdid = (String) doc.getFieldValue(SOLR_FIELD_FILE_DOWNLOAD_ID); 
	            result += this.dataAccessApiBaseUrl + fdid + "\n";
	        }
	        
        } catch(Exception e) {
        	// send 500 "Internal Server Error" response
        	e.printStackTrace();
        	LOG.warning(e.getMessage());
        	return Response.status(500).entity(e.getMessage()).build();
        }
				
		return Response.status(200).entity(result).build();
				
	}

}
