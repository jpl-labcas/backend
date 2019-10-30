package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import gov.nasa.jpl.labcas.data_access_api.utils.UrlUtils;

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
	 * Proxies the HTTP query request to a specific Solr core,
	 * after adding the access control constraints.
	 * 
	 * @param httpRequest
	 * @param core
	 * @return
	 */
	private Response queryCore(@Context HttpServletRequest httpRequest, ContainerRequestContext requestContext, String core) {
		
		try {
			
			String baseUrl = getBaseUrl(core) + "/select";
			String url = baseUrl + "?" + httpRequest.getQueryString() + getAccessControlString(requestContext);
			return SolrProxy.query(url);
			
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
		
		String fqv = getAccessControlQueryStringValue(requestContext);
		if (!fqv.isEmpty()) {
			// must URL-encode the parameter value
			return "&fq="+UrlUtils.encode(fqv);
		} else {
			return "";
		}
		
	}

}
