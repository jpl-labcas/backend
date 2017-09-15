package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.codec.binary.Base64;

/**
 * Filter that intercepts all requests to this service
 * and verifies authentication versus the LDAP database.
 * 
 * @author Luca Cinquini
 *
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
	
	public final static String USER_GROUPS_PROPERTY = "userGroups";
	
	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
	
	private UserService userService = new UserServiceLdapImpl();
	
	@Override
	public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {
		
		String userdn = null;
		
		// extract username and password from encoded HTTP 'Authorization' header
		// header value format will be "Basic encodedstring" for Basic authentication.
		// Example "Basic YWRtaW46YWRtaW4="
		String authCredentials = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
		LOG.info("Establishing authentication: HTTP header="+authCredentials);
		
		if (authCredentials!=null) {
			
			final String encodedUserPassword = authCredentials.replaceFirst("Basic" + " ", "");
			String usernameAndPassword = null;
			
			try {
				
				byte[] decodedBytes = Base64.decodeBase64(encodedUserPassword.getBytes());
				usernameAndPassword = new String(decodedBytes, "UTF-8");
				
				final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
				final String username = tokenizer.nextToken();
				final String password = tokenizer.nextToken();
	
				userdn = userService.getValidUser(username, password);
				LOG.info("Retrieved user DN = "+userdn);
				
				if (userdn!=null) {
					
					List<String> ugroups = userService.getUserGroups(userdn);
					containerRequest.setProperty(USER_GROUPS_PROPERTY, ugroups);
					LOG.info("Storing in request: user groups = "+ugroups);
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		if (userdn==null) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
		// else proceed with normal filter chain

	}
}