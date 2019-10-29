package gov.nasa.jpl.labcas.data_access_api.service;

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
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
	public void create(@Context HttpServletRequest httpRequest,
			@Context ContainerRequestContext requestContext, 
			@Context HttpHeaders headers,
			String document) {
		
		LOG.info("/userdata/create request: " + document);
		String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
		LOG.info("/userdata/create content type: " + contentType);

	}

}
