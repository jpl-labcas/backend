package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import gov.nasa.jpl.labcas.data_access_api.utils.HttpClient;

/**
 * Service implementation to operate on user data.
 */
@Path("/userdata")
@Produces(MediaType.TEXT_PLAIN)
public class UserDataServiceImpl extends SolrProxy implements UserDataService {
	
	private final static Logger LOG = Logger.getLogger(UserDataServiceImpl.class.getName());
	
	public UserDataServiceImpl() {
		super();
	}

	@Override
	@POST
	@Path("/create")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
	public Response create(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext, 
			@Context HttpHeaders headers,
			String document) {
		
		try {
			
			// proxy the client HTTP request to Solr as-is
			// use the same content-type header
			LOG.info("/userdata/create request: " + document);
			String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
			LOG.info("/userdata/create content type: " + contentType);
			HttpClient httpClient = new HttpClient();
			String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/update/json/docs";
			String response = httpClient.doPost(new URL(url), document, contentType);
			LOG.info("/userdata/create response: " + response);
			
			// send Solr response back to client
			return Response.status(200).entity(response).build();
		
		} catch(Exception e) {
			// return HTTP "Server Error" response back to client
			LOG.warning("HTTP Post error:" + e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}
		
	}

}
