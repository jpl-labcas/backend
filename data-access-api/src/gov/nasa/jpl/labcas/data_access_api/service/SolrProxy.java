package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.SolrServer;

import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;
import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

/**
 * Base class to proxy query/download/metadata requests to a multi-core Solr server.
 * 
 */
public class SolrProxy {
	
	private final static Logger LOG = Logger.getLogger(SolrProxy.class.getName());

	// default base Solr URL if $FILEMGR_URL is not set
	protected final static String SOLR_URL_PROPERTY = "solrUrl";
	static String SOLR_URL = "https://localhost:8984/solr";
	
	protected final static String SOLR_CORE_COLLECTIONS = "collections";
	protected final static String SOLR_CORE_DATASETS = "datasets";
	protected final static String SOLR_CORE_FILES = "files";
	protected final static String SOLR_CORE_USERDATA = "userdata";
	
	protected final static String SOLR_FIELD_ID = "id";
	protected final static String SOLR_FIELD_DATASET_ID = "DatasetId";
	protected final static String SOLR_FIELD_COLLECTION_ID = "CollectionId";
	protected final static String SOLR_FIELD_FILE_NAME = "FileName";
	protected final static String SOLR_FIELD_NAME = "name";
	protected final static String SOLR_FIELD_FILE_LOCATION = "FileLocation";
	protected final static int SOLR_MAX_NUM_FILES = 100;  // maximum number of files returned for each query
	
	// access control metadata fields
	protected final static String SUPER_OWNER_PRINCIPAL_PROPERTY = "superOwnerPrincipal";
	protected static String superOwnerPrincipal;
	protected final static String PUBLIC_OWNER_PRINCIPAL_PROPERTY = "publicOwnerPrincipal";
	protected static String publicOwnerPrincipal;

	// These characters are not allowed in HTTP request parameter values 
	// - AFTER the value has been URL-decoded
	protected final static String[] UNSAFE_CHARACTERS = new String[] { ">", "<", "%", "$" };
	
	protected static String UNSAFE_CHARACTERS_MESSAGE = "HTTP request contains unsafe characters";

	// IMPORTANT: must re-use the same SolrServer instance across all requests to prevent memory leaks
	// see https://issues.apache.org/jira/browse/SOLR-861
	// This method instantiates the shared instances of SolrServer (one per core)
	protected static Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();

	static {
		try {
			// Since Solr is using a self-signed cert on https://localhost, we need to ignore cert errors:
			Protocol https = new Protocol("https", new InsecureSocketFactory(), 443);
			Protocol.registerProtocol("https", https);

			SOLR_URL = Parameters.getParameterValue(SOLR_URL_PROPERTY);
			solrServers.put(SOLR_CORE_COLLECTIONS, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_COLLECTIONS)));
			solrServers.put(SOLR_CORE_DATASETS, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_DATASETS)));
			solrServers.put(SOLR_CORE_FILES, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_FILES)));
			solrServers.put(SOLR_CORE_USERDATA, new CommonsHttpSolrServer(getBaseUrl(SOLR_CORE_USERDATA)));

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
	static String getBaseUrl(String core) {
		
		return SOLR_URL + "/" + core;
	}
	
	/**
	 * Method to send a generic HTTP query request to Solr and return the HTTP query response.
	 * 
	 * NOTE: this method uses the HTTPClient API directly because SolrJ does not allow to return 
	 * the raw response document as JSON or XML without a lot of processing.
	 * 
	 * @param url: the complete query URL (must be URL-encoded)
	 * @param core
	 * @return
	 */
	static Response query(String url) {
		
		try {
			CloseableHttpClient httpclient = null;
			try {
				httpclient = HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build())
					.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				System.err.println("I give up in SolrProxy.query");
				System.exit(42);
			}

			LOG.info("Executing Solr HTTP request: " + url);
			HttpGet httpGet = new HttpGet(url);
			CloseableHttpResponse response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			// return the same response to the client
			String content = IOUtils.toString(entity.getContent(), "UTF-8");
			return Response.status(response.getStatusLine().getStatusCode()).entity(content).build();

		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}
	
	/**
	 * Builds the query string to enforce access control.
	 * Note that this method does NOT URL-encode the parameter value:
	 * that is left to do by the calling method.
	 * @param contex
	 * @return
	 */
	static String getAccessControlQueryStringValue(ContainerRequestContext requestContext) throws UnsupportedEncodingException {
		
		@SuppressWarnings("unchecked")
		List<String> ugroups = (List<String>)requestContext.getProperty(AuthenticationFilter.USER_GROUPS_PROPERTY);
		LOG.info("Retrieving from request context: user groups = "+ugroups);
		String accessControlQueryStringValue = "";
				
		if (ugroups!=null && ugroups.size()>0) {
			
			if (ugroups.contains(superOwnerPrincipal)) {
				// super user --> no query constraint
				//String testOwnerPrincipal = "uid=testuser,dc=edrn,dc=jpl,dc=nasa,dc=gov";
				//return "OwnerPrincipal:(\""+testOwnerPrincipal+"\"" + ")";
				LOG.info("🦸 SUPER USER DETECTED (play triumphant theme here 🎶)");
				return "";
			} else {
				accessControlQueryStringValue = "OwnerPrincipal:(\""+publicOwnerPrincipal+"\"";
				for (String ugroup : ugroups) {
					accessControlQueryStringValue += " OR \"" + ugroup + "\"";
				}
				accessControlQueryStringValue += ")";
			}
			
		} else {
			// no groups --> can only read public data
			accessControlQueryStringValue = "OwnerPrincipal:(\""+publicOwnerPrincipal+"\")";
		}
		
		return accessControlQueryStringValue;
		
	}
	
	/**
	 * Method to check whether a string contains unsafe characters.
	 * @param string
	 * @return
	 */
	static boolean isSafe(String string) {
		
		for (String s : UNSAFE_CHARACTERS) {
			if (string.contains(s)) {
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * Method to check whether any element of a collection 
	 * of strings contains unsafe characters.
	 * @param strings
	 * @return
	 */
	static boolean isSafe(Collection<String> strings) {
		
		for (String str : strings) {
			if (!isSafe(str)) {
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * Method to check all HTTP parameter values for unsafe characters.
	 * @param httpRequest
	 * @return
	 */
	static boolean isSafe(HttpServletRequest httpRequest) {
		
		Map<String, String[]> params = httpRequest.getParameterMap();
		
		for (String key : params.keySet()) {
			for (String value : params.get(key)) {
				if (!isSafe(value)) {
					return false;
				}
			}
		}
		return true;
		
	}
	
	public SolrProxy() {}
	
}
