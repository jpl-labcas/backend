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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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
			HttpClient httpClient = new HttpClient();
			String url = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/update/json/docs?commit=true";
			
			// return the HTTP response as-is to the client
			// (including possible error)
			return httpClient.doPost(url, document, contentType);
		
	}
	
	@Override
	@GET
	@Path("/read")
	public Response read(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext,
			@QueryParam("id") String id) {
		
		LOG.info("/userdata/read request: id="+id);
		
		try {
			String baseUrl = getBaseUrl(SolrProxy.SOLR_CORE_USERDATA) + "/select";
			String url = baseUrl + "?wt=json&q=id:"+id;
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

}
