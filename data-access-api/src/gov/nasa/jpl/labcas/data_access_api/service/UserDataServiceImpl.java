package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;
import gov.nasa.jpl.labcas.data_access_api.utils.HttpClient;
import gov.nasa.jpl.labcas.data_access_api.utils.UrlUtils;

/**
 * Service implementation to operate on user data.
 */
@Path("/userdata")
@Produces(MediaType.APPLICATION_JSON)
public class UserDataServiceImpl extends SolrProxy implements UserDataService {
	
	private final static Logger LOG = Logger.getLogger(UserDataServiceImpl.class.getName());
	
	private static Pattern DN_PATTERN = Pattern.compile(".*uid=([^,]+).*"); 
	
	public UserDataServiceImpl() {
		super();
	}

	@Override
	@POST
	@Path("/create")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response create(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext, 
			@Context HttpHeaders headers,
			String document) {
		
		// check request for unsafe input
		if (!isSafe(document)) {
			return Response.status(Status.BAD_REQUEST).entity("Request contains unsafe characters").build();
		}
		
		String dn = (String)requestContext.getProperty(AuthenticationFilter.USER_DN);
		
		// parse input json document to retrieve the user id
		JSONObject jobj = new JSONObject(document);
		if (jobj.has("id")) {
			
			String id = jobj.getString("id");
						
			if (authorize(dn, id)) {
				
				// proxy the client HTTP request to Solr as-is
				// use the same content-type header
				LOG.info("/userdata/create request: " + document);
				String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
				LOG.info("/userdata/create content type: " + contentType);
				String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/update/json/docs?commit=true";
				
				// return the HTTP response as-is to the client
				// (including possible error)
				HttpClient httpClient = new HttpClient();
				return httpClient.doPost(url, document, contentType);
				
			} else {
				
				String message = "User: " + dn + " is not authorized to create data for user: "+id;
				return Response.status(Status.UNAUTHORIZED).entity(message).build();
				
			}
			
		} else {
			
			return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
			
		}
		
	}
	
	@Override
	@GET
	@Path("/read")
	public Response read(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext,
			@QueryParam("id") String id) {
		
		if (id==null) {
			return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
		} else if (!isSafe(id)) {
			return Response.status(Status.BAD_REQUEST).entity("'id' contains unsafe characters").build();
		}
		
		String dn = (String)requestContext.getProperty(AuthenticationFilter.USER_DN);
		
		if (authorize(dn, id)) {
			
			LOG.info("/userdata/read request: id="+id);
			try {
				String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/select?wt=json&q="+UrlUtils.encode("id:"+id);
				return SolrProxy.query(url);
			} catch(UnsupportedEncodingException e) {
				return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
			}
			
		} else {
			
			String message = "User: " + dn + " is not authorized to read data for user: "+id;
			return Response.status(Status.UNAUTHORIZED).entity(message).build();
			
		}
		
	}
	
	@Override
	@POST
	@Path("/delete")
	public Response delete(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext,
			@QueryParam("id") String id) {
		
		if (id==null) {
			return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
		} else if (!isSafe(id)) {
			return Response.status(Status.BAD_REQUEST).entity("'id' contains unsafe characters").build();
		}
		
		String dn = (String)requestContext.getProperty(AuthenticationFilter.USER_DN);
		
		if (authorize(dn, id)) {
		
			// build HTTP Post request
			LOG.info("/userdata/delete request: id="+id);
			String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/update?commit=true";
			String doc = "{'delete': {'query': 'id:"+id+"'} }";
			
			// execute HTTP Post request and return HTTP response
			HttpClient httpClient = new HttpClient();
			return httpClient.doPost(url, doc, MediaType.APPLICATION_JSON);
		
		} else {
			
			String message = "User: " + dn + " is not authorized to delete data for user: "+id;
			return Response.status(Status.UNAUTHORIZED).entity(message).build();
			
		}
		
	}
	
	/**
	 * Verifies that the identify of the user submitting the HTTP request ("userDn") 
	 * matches the user identifier of the data to operate on ("userId")
	 * 
	 * @param usedDn: user Distinguished Name retrieve by the authentication filter
	 * @param userId: user identifier embedded with the HTTP request
	 * @return
	 */
	private boolean authorize(String usedDn, String userId) {
		
		Matcher m = DN_PATTERN.matcher(usedDn);
		if (m.matches()) {
			if (m.group(1).equals(userId)) {
				return true;
			}
		}
		return false;
	}
	


}
