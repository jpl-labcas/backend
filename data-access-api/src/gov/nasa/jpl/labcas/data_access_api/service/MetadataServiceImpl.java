package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Service implementation to update the LabCAS metadata
 * 
 * @author Luca Cinquini
 *
 */
@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class MetadataServiceImpl extends SolrProxy implements MetadataService {
	
	private final static Logger LOG = Logger.getLogger(MetadataServiceImpl.class.getName());

	public MetadataServiceImpl() {
		super();
	}

	@Override
	@GET
	@Path("/updateById")
	public Response updateById(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
			@QueryParam("core") String core, @QueryParam("action") String action, @QueryParam("id") String id,
			@QueryParam("field") String field, @QueryParam("value") List<String> value) {
		
		LOG.info("UpdateById request: core="+core+" action="+action+" id="+id+" field="+field+" value="+value);
		
		return Response.status(200).entity("OK").build();
	}

	@Override
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_XML)
	public Response update(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext, String content) {
		// TODO Auto-generated method stub
		LOG.info(content);
		return Response.status(200).entity("OK").build();
	}

}
