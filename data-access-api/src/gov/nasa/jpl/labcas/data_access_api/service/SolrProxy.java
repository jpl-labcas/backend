package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

/**
 * Base class to proxy query/download requests to a multi-core Solr server.
 * 
 * @author Luca Cinquini
 *
 */
public class SolrProxy {
	
	private final static Logger LOG = Logger.getLogger(SolrProxy.class.getName());

	// default base Solr URL if $FILEMGR_URL is not set
	protected static String SOLR_URL = "http://localhost:8983/solr";
	protected final static String SOLR_CORE_COLLECTIONS = "collections";
	protected final static String SOLR_CORE_DATASETS = "datasets";
	protected final static String SOLR_CORE_FILES = "files";
	protected final static String SOLR_FIELD_ID = "id";
	protected final static String SOLR_FIELD_DATASET_ID = "DatasetId";
	protected final static String SOLR_FIELD_COLLECTION_ID = "CollectionId";
	protected final static int SOLR_MAX_NUM_FILES = 100;  // maximum number of files returned for each query


	// IMPORTANT: must re-use the same SolrServer instance across all requests
	// to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861
	// this method instantiates the shared instances of SolrServer (one per
	// core)
	protected static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();

	static {
		try {

			//if (System.getenv("FILEMGR_URL") != null) {
			//	SOLR_URL = System.getenv("FILEMGR_URL").replaceAll("9000", "8983") + "/solr";
			//}
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
	
	public SolrProxy() {}
	
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
	protected SolrQuery buildPassThroughQuery(final HttpServletRequest httpRequest, 
			                                  final String q, final List<String> fq,
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
		
		return request;

	}


}
