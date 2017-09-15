package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;

/**
 * Service implementation to issue a query request to Solr.
 * 
 * @author Luca Cinquini
 *
 */
@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class QueryServiceImpl extends SolrProxy implements QueryService {

	private final static Logger LOG = Logger.getLogger(QueryServiceImpl.class.getName());

	public QueryServiceImpl() {
		super();
	}

	@Override
	@GET
	@Path("/collections/select")
	public Response queryCollections(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext) {
				
		return queryCore(httpRequest, requestContext, SOLR_CORE_COLLECTIONS);
		
	}

	@Override
	@GET
	@Path("/datasets/select")
	public Response queryDatasets(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext) {
		
		return queryCore(httpRequest, requestContext, SOLR_CORE_DATASETS);
		
	}

	@Override
	@GET
	@Path("/files/select")
	public Response queryFiles(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext) {
		
		return queryCore(httpRequest, requestContext, SOLR_CORE_FILES);

	}
	
	/**
	 * Proxies the HTTP request to a specific Solr core.
	 * NOTE: this method uses the HTTPClient API directly 
	 * because SolrJ does not allow to return the raw response document as JSON or XML
	 * without a lot of processing.
	 * 
	 * @param httpRequest
	 * @param core
	 * @return
	 */
	private Response queryCore(@Context HttpServletRequest httpRequest, ContainerRequestContext requestContext, String core) {
		
		try {
			String baseUrl = getBaseUrl(core) + "/select";
			String url = baseUrl + "?" + httpRequest.getQueryString() + getAccessControlString(requestContext);
			CloseableHttpClient httpclient = HttpClients.createDefault();
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
			return Response.status(500).entity(e.getMessage()).build();
		}

	}
	
	/**
	 * Method to build the access control constraint
	 * Example: fq=OwnerPrincipal:("cn=Spira Boston University,ou=groups,o=MCL" OR "cn=Amos Geisel School of Medicine,ou=groups,o=MCL")
	 * 
	 * @param requestContext
	 * @return
	 */
	private String getAccessControlString(ContainerRequestContext requestContext) throws UnsupportedEncodingException {
		
		@SuppressWarnings("unchecked")
		List<String> ugroups = (List<String>)requestContext.getProperty(AuthenticationFilter.USER_GROUPS_PROPERTY);
		LOG.info("Retrieving from request context: user groups = "+ugroups);
		String fqv = "";
		
		// FIXME
		ugroups = new ArrayList<String>();
		ugroups.add("cn=Borowsky University of California Davis,ou=groups,o=MCL");
		ugroups.add("cn=Spira Boston University,ou=groups,o=MCL");
		
		if (ugroups!=null && ugroups.size()>0) {
			
			if (ugroups.contains(superOwnerPrincipal)) {
				// super user --> no query constraint
				return "";
			} else {
				fqv = "OwnerPrincipal:(\""+publicOwnerPrincipal+"\"";
				for (String ugroup : ugroups) {
					fqv += " OR \"" + ugroup + "\"";
				}
				fqv += ")";
			}
			
		} else {
			// no groups --> can only read public data
			fqv = "OwnerPrincipal:(\""+publicOwnerPrincipal+"\")";
		}
		
		String fq = "&fq=" + URLEncoder.encode(fqv, "UTF-8");
		LOG.info("Access control string: "+fq);
		return fq;
		
	}

}
