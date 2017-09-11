package gov.nasa.jpl.labcas.data_access_api.filter;

import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
	
	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
	
	private AuthenticationService authenticationService;
	
	public AuthenticationFilter(AuthenticationService authenticationService) {
		this.authenticationService = new AuthenticationService();
	}

	@Override
	public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {
		
		String authCredentials = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
		LOG.info("Establishing authentication: HTTP header="+authCredentials);

		boolean authenticationStatus = authenticationService.authenticate(authCredentials);

		if (!authenticationStatus) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}

	}
}