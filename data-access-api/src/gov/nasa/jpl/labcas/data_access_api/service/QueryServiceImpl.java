package gov.nasa.jpl.labcas.data_access_api.service;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
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

	// Parameters that could be used for SSRF attacks - these will be filtered out
	private final static String[] DANGEROUS_PARAMETERS = new String[] {
		"shards",           // Can point to arbitrary Solr servers
		"shards.qt",        // Can specify query template on remote shards
		"stream.url",       // Can stream from arbitrary URLs
		"stream.file",      // Can read arbitrary files
		"stream.body"       // Can execute arbitrary code
	};

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
		// jpl-labcas/backend#29
		//
		// Require a logged-in user to query file metadata

		String distinguishedName = (String) requestContext.getProperty(AuthenticationFilter.USER_DN);
		LOG.info("🪪 the DN for queryFiles (/files/select) is «" + distinguishedName + "»");

		if (distinguishedName == null || distinguishedName.equals(AuthenticationFilter.GUEST_USER_DN)) {
			LOG.info("VDP_1645_SC-9999-L-JPL-0220 violation: login required to query (even for public data)");
			return Response.status(Status.UNAUTHORIZED)
				.entity("User login required to query file metadata (even for public data, so there!)").build();
		}

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

			// EDRN/labcas-ui#254: we do want to allow guests to query *public* data. The front-end
			// Apache HTTPD has mod_evasive installed which should help prevent mass-exfiltration.
			// Therefore, I'm commenting this out. However, if the security red team balks and wants
			// EDRN/Infrastructure#144 back in action, we can re-enable it.
			//
			// if (distinguishedName == null || distinguishedName.equals(AuthenticationFilter.GUEST_USER_DN)) {
			// 	LOG.info("VDP_1645_SC-9999-L-JPL-0220 violation: login required to query (even for public data)");
			// 	return Response.status(Status.UNAUTHORIZED)
			// 		.entity("User login required to query (even for public data)").build();
			// }

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
			
			// Filter out dangerous parameters that could be used for SSRF attacks
			String safeQueryString = buildSafeQueryString(httpRequest);
			String accessControlString = getAccessControlString(requestContext);
			
			String baseUrl = getBaseUrl(core) + "/select";
			// Build URL: if safeQueryString is empty, use accessControlString directly (it starts with &)
			// Otherwise, combine them with &
			String url;
			if (safeQueryString.isEmpty()) {
				url = baseUrl + (accessControlString.isEmpty() ? "" : "?" + accessControlString.substring(1));
			} else {
				url = baseUrl + "?" + safeQueryString + accessControlString;
			}
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
	
	/**
	 * Builds a safe query string by filtering out dangerous parameters that could be used
	 * for SSRF attacks (e.g., shards, stream.url, etc.).
	 * 
	 * @param httpRequest
	 * @return A safe query string with dangerous parameters removed
	 */
	private String buildSafeQueryString(HttpServletRequest httpRequest) throws UnsupportedEncodingException {
		StringBuilder safeQuery = new StringBuilder();
		Enumeration<String> paramNames = httpRequest.getParameterNames();
		boolean first = true;
		
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			
			// Check if this parameter is dangerous
			boolean isDangerous = false;
			for (String dangerous : DANGEROUS_PARAMETERS) {
				// Check exact match or if parameter starts with dangerous prefix (e.g., "shards.qt")
				if (paramName.equals(dangerous) || paramName.startsWith(dangerous + ".")) {
					isDangerous = true;
					LOG.warning("⚠️ Blocked dangerous parameter: " + paramName);
					break;
				}
			}
			
			if (!isDangerous) {
				String[] values = httpRequest.getParameterValues(paramName);
				for (String value : values) {
					if (!first) {
						safeQuery.append("&");
					}
					first = false;
					// URL-encode parameter name and value
					safeQuery.append(UrlUtils.encode(paramName));
					safeQuery.append("=");
					safeQuery.append(UrlUtils.encode(value));
				}
			}
		}
		
		return safeQuery.toString();
	}

}
