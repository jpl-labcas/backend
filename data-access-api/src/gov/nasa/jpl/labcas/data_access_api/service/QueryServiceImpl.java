package gov.nasa.jpl.labcas.data_access_api.service;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
	private final static String MAX_SOLR_ROWS_PROPERTY = "max_solr_rows";
	private final int maxRows;

	public QueryServiceImpl() {
		super();
		maxRows = Integer.parseInt(Parameters.getParameterValue(MAX_SOLR_ROWS_PROPERTY));
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
			// VDP_1645_SC-9999-L-JPL-0220 — ensure credentials are always provided
			String distinguishedName = (String) requestContext.getProperty(AuthenticationFilter.USER_DN);
			LOG.info("🪪 the distinguishedName is «" + distinguishedName + "»");
			if (distinguishedName == null || distinguishedName.equals(AuthenticationFilter.GUEST_USER_DN)) {
				LOG.info("VDP_1645_SC-9999-L-JPL-0220 violation: login required to query (even for public data)");
				return Response.status(Status.UNAUTHORIZED)
					.entity("User login required to query (even for public data)").build();
			}

			// check request for unsafe characters
			// must URL-decode the query string first
			String q = URLDecoder.decode(httpRequest.getQueryString(), "UTF-8");
			if (!isSafe(q)) {
				return Response.status(Status.BAD_REQUEST).entity(UNSAFE_CHARACTERS_MESSAGE).build();
			}

			// VDP_1645_SC-9999-L-JPL-0220; limit the rows
			String rowsParam = httpRequest.getParameter("rows");
			if (rowsParam != null) {
				try {
					int rows = Integer.parseInt(rowsParam);
					if (rows > maxRows) {
						return Response.status(Status.BAD_REQUEST)
							.entity("«rows» must be ≤" + maxRows).build();
					}
				} catch (NumberFormatException ex) {
					return Response.status(Status.BAD_REQUEST).entity("«rows» must be a valid integer").build();
				}
			}
			
			String baseUrl = getBaseUrl(core) + "/select";
			String url = baseUrl + "?" + httpRequest.getQueryString() + getAccessControlString(requestContext);
			LOG.info("🕵️‍♀️ Executing query: "+url);
			return SolrProxy.query(url);
		} catch (RuntimeException ex) {
			// Java "gotcha" that only I seem to know about
			throw ex;
		} catch (Exception ex) {
			// send 500 "Internal Server Error" response
			ex.printStackTrace();
			LOG.warning(ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
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
