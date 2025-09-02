package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;
import gov.nasa.jpl.labcas.data_access_api.jwt.Sessions;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class TokenServiceImpl implements TokenService  {
	
	//private final static Logger LOG = Logger.getLogger(TokenServiceImpl.class.getName());
	
	public TokenServiceImpl() {}

	@Override
	@POST
	@Path("/auth")
	public Response auth(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext) {
		
		// retrieve token from context
		String token = (String)requestContext.getProperty(AuthenticationFilter.JWT);
		
		// send as plain text response
		return Response.status(Status.OK).entity(token).build();
		
	}

	@Override
	@GET
	@Path("/logout")
	public Response logout(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext, @QueryParam("sessionID") String sessionID) {
		Sessions.INSTANCE.endSession(sessionID);
		return Response.status(Status.OK).entity("Logged out").build();
	}
}
