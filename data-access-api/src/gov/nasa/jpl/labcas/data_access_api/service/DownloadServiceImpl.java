package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
	private final static String SOLR_FIELD_ID = "id";
	private final static String SOLR_FIELD_DATASET_ID = "DatasetId";
	private final static String SOLR_FIELD_COLLECTION_ID = "CollectionId";

	/**
	 * Configuration file located in user home directory
	 */
	private final static String LABCAS_PROPERTIES = "/labcas.properties";
	private final static String DATA_ACCESS_API_BASE_URL_PROPERTY = "dataAccessApiBaseUrl";

	// IMPORTANT: must re-use the same SolrServer instance across all requests
	// to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861
	// this method instantiates the shared instances of SolrServer (one per
	// core)
	private static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();

	static {
		try {

			if (System.getenv("FILEMGR_URL") != null) {
				SOLR_URL = System.getenv("FILEMGR_URL").replaceAll("9000", "8983") + "/solr";
			}
			// FIXME
			SOLR_URL = "https://mcl-labcas.jpl.nasa.gov/solr";
			LOG.info("Using base SOLR_URL=" + SOLR_URL);

			solrServers.put(SOLR_CORE_COLLECTIONS, new CommonsHttpSolrServer(SOLR_URL + "/" + SOLR_CORE_COLLECTIONS));
			solrServers.put(SOLR_CORE_DATASETS, new CommonsHttpSolrServer(SOLR_URL + "/" + SOLR_CORE_DATASETS));
			solrServers.put(SOLR_CORE_FILES, new CommonsHttpSolrServer(SOLR_URL + "/" + SOLR_CORE_FILES));

		} catch (MalformedURLException e) {
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
			LOG.info("Using dataAccessApiBaseUrl=" + this.dataAccessApiBaseUrl);

		} catch (IOException e) {
			LOG.warning("Eroor reading property file: " + LABCAS_PROPERTIES);
		}

	}

	@Override
	@GET
	@Path("/collections/select")
	public Response downloadCollections(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {
		// FIXME
		return null;
	}

	@Override
	@GET
	@Path("/datasets/select")
	public Response downloadDatasets(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {

		// build Solr query to 'datasets' core
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows);

		// execute Solr query to 'datasets' core
		// extract matching dataset ids
		List<String> datasetIds = new ArrayList<String>();
		try {

			QueryResponse response = solrServers.get(SOLR_CORE_DATASETS).query(request);
			this.extractIds(response, datasetIds);

		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		// final results document
		String results = "";

		if (datasetIds.size() > 0) {
			
			SolrQuery request2 = this.buildFilesQuery(SOLR_FIELD_DATASET_ID, datasetIds);

			// execute query to 'files' core
			try {
				
				QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request2);
				results = buildResultsDocument(response);
				
			} catch (Exception e) {
				// send 500 "Internal Server Error" response
				e.printStackTrace();
				LOG.warning(e.getMessage());
				return Response.status(500).entity(e.getMessage()).build();
			}

		}

		return Response.status(200).entity(results).build();

	}

	@Override
	@GET
	@Path("/files/select")
	public Response downloadFiles(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {

		// build Solr query
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows); 

		// execute Solr query to 'files' core, build result document
		String results = "";
		try {

			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			results = buildResultsDocument(response);
			
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		return Response.status(200).entity(results).build();

	}

	/**
	 * Method that converts a query request to this service to a query request to the Solr server (for any core).
	 * Example query: 
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @param sort
	 * @param fields
	 * @return
	 */
	private SolrQuery buildPassThroughQuery(final HttpServletRequest httpRequest, final String q, final List<String> fq,
			final int start, final int rows) {

		LOG.info("HTTP request URL=" + httpRequest.getRequestURL());
		LOG.info("HTTP request query string=" + httpRequest.getQueryString());
		LOG.info("HTTP request parameters q=" + q + " fq=" + fq + " start=" + start + " rows=" + rows);

		// build Solr query
		SolrQuery request = new SolrQuery();
		if (q != null && !q.isEmpty()) {
			request.setQuery(q);
		}
		if (fq != null && fq.size() > 0) {
			request.setFilterQueries(fq.toArray(new String[fq.size()]));
		}
		if (start > 0) {
			request.setStart(start);
		}
		if (rows > 0) {
			request.setRows(rows);
		}
		// add fields to be returned
		request.setFields( new String[] { SOLR_FIELD_ID } );
		// always sort by result "id"
		request.setSortField(SOLR_FIELD_ID, ORDER.desc);

		LOG.info("Executing Solr request:"+request.toString());
		
		return request;

	}
	
	/**
	 * Method that builds a query for files matching a set of DatasetIds or CollectionIds.
	 * 
	 * Example: q=DatasetId%3A%28%22MD_Anderson_Pancreas_IPMN_images.IPMN_P1-06_H03%22%29&fl=id%2CFileDownloadId&rows=10000
	 * 
	 * @return
	 */
	private SolrQuery buildFilesQuery(final String idKey, final List<String> idValues) {
		
		SolrQuery request = new SolrQuery();
		String idquery = idKey + ":(";
		for (int i = 0; i < idValues.size(); i++) {
			if (i > 0) {
				idquery += " OR ";
			}
			idquery += "\"" + idValues.get(i) + "\"";
		}
		idquery += ")";
		request.setQuery(idquery);
		// add fields to be returned
		request.setFields( new String[] { SOLR_FIELD_ID } );
		// FIXME
		request.setRows(10000);
		
		LOG.info("Executing Solr request:"+request.toString());
		return request;
		
	}
	
	/**
	 * Method that parses a Solr response to extract the result ids.
	 * @param response
	 * @param ids
	 */
	private void extractIds(final QueryResponse response, List<String> ids) {
		SolrDocumentList docs = response.getResults();
		Iterator<SolrDocument> iter = docs.iterator();
		while (iter.hasNext()) {
			SolrDocument doc = iter.next();
			LOG.info(doc.toString());
			String id = (String) doc.getFieldValue(SOLR_FIELD_ID);
			ids.add(id);
		}
	}
	
	/**
	 * Method that parses a Solr response from the 'files' core and builds a list of files download URLs.
	 * 
	 * @return
	 */
	private String buildResultsDocument(final QueryResponse response) {
		
		String results = "";
		SolrDocumentList docs = response.getResults();
		Iterator<SolrDocument> iter = docs.iterator();
		while (iter.hasNext()) {
			SolrDocument doc = iter.next();
			LOG.fine(doc.toString());
			String id = (String) doc.getFieldValue(SOLR_FIELD_ID);
			results += this.dataAccessApiBaseUrl + "?id=" + id + "\n";
		}
		
		return results;
		
	}

}
