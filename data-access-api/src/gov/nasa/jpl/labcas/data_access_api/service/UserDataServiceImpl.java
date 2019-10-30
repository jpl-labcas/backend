package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.logging.Logger;

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

import gov.nasa.jpl.labcas.data_access_api.utils.HttpClient;

/**
 * Service implementation to operate on user data.
 */
@Path("/userdata")
@Produces(MediaType.APPLICATION_JSON)
public class UserDataServiceImpl extends SolrProxy implements UserDataService {
	
	private final static Logger LOG = Logger.getLogger(UserDataServiceImpl.class.getName());
	
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
		
	}
	
	@Override
	@GET
	@Path("/read")
	public Response read(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext,
			@QueryParam("id") String id) {
		
		LOG.info("/userdata/read request: id="+id);
		String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/select?wt=json&q=id:"+id;
		return SolrProxy.query(url);
		
	}
	
	@Override
	@POST
	@Path("/delete")
	public Response delete(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext,
			@QueryParam("id") String id) {
		
		// build HTTP Post request
		LOG.info("/userdata/delete request: id="+id);
		String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/update?commit=true";
		String doc = "{'delete': {'query': 'id:"+id+"'} }";
		
		// execut HTTP Post request and return HTTP response
		HttpClient httpClient = new HttpClient();
		return httpClient.doPost(url, doc, MediaType.APPLICATION_JSON);
		
	}

}
