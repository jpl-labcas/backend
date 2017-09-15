package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

/**
 * Base class to proxy query/download requests to a multi-core Solr server.
 * 
 * @author Luca Cinquini
 *
 */
public class SolrProxy {
	
	private final static Logger LOG = Logger.getLogger(SolrProxy.class.getName());

	// default base Solr URL if $FILEMGR_URL is not set
	protected final static String SOLR_URL_PROPERTY = "solrUrl";
	static String SOLR_URL = "http://localhost:8983/solr";
	
	protected final static String SOLR_CORE_COLLECTIONS = "collections";
	protected final static String SOLR_CORE_DATASETS = "datasets";
	protected final static String SOLR_CORE_FILES = "files";
	protected final static String SOLR_FIELD_ID = "id";
	protected final static String SOLR_FIELD_DATASET_ID = "DatasetId";
	protected final static String SOLR_FIELD_COLLECTION_ID = "CollectionId";
	protected final static int SOLR_MAX_NUM_FILES = 100;  // maximum number of files returned for each query
	
	// access control metadata fields
	protected final static String SUPER_OWNER_PRINCIPAL_PROPERTY = "superOwnerPrincipal";
	protected static String superOwnerPrincipal;
	protected final static String PUBLIC_OWNER_PRINCIPAL_PROPERTY = "publicOwnerPrincipal";
	protected static String publicOwnerPrincipal;


	// IMPORTANT: must re-use the same SolrServer instance across all requests
	// to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861
	// this method instantiates the shared instances of SolrServer (one per
	// core)
	protected static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();

	static {
		try {
			SOLR_URL = Parameters.getParameterValue(SOLR_URL_PROPERTY);
			solrServers.put(SOLR_CORE_COLLECTIONS, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_COLLECTIONS)));
			solrServers.put(SOLR_CORE_DATASETS, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_DATASETS)));
			solrServers.put(SOLR_CORE_FILES, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_FILES)));

		} catch (MalformedURLException e) {
			e.printStackTrace();
			LOG.warning(e.getMessage());
		}
		
		superOwnerPrincipal = Parameters.getParameterValue(SUPER_OWNER_PRINCIPAL_PROPERTY);
		publicOwnerPrincipal= Parameters.getParameterValue(PUBLIC_OWNER_PRINCIPAL_PROPERTY);
		
	}
	
	/**
	 * Builds the base URL for a given core.
	 * Example: https://mcl-labcas.jpl.nasa.gov/solr/collections
	 * 
	 * @param core
	 * @return
	 */
	static public String getBaseUrl(String core) {
		
		return SOLR_URL + "/" + core;
	}
	
	public SolrProxy() {}
	
}
