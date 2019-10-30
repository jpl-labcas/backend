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

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import gov.nasa.jpl.labcas.data_access_api.exceptions.MissingAuthenticationHeaderException;
import gov.nasa.jpl.labcas.data_access_api.jwt.JwtConsumer;
import gov.nasa.jpl.labcas.data_access_api.jwt.JwtProducer;

/**
 * Filter that intercepts all requests to this service
 * and verifies authentication versus the LDAP database.
 * 
 * @author Luca Cinquini
 *
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
//@PreMatching
public class AuthenticationFilter implements ContainerRequestFilter {
	
	public final static String JWT = "JasonWebToken";
	public final static String USER_GROUPS_PROPERTY = "userGroups";
	public final static String USER_DN = "userDn";
	
	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
	
	private UserService userService = new UserServiceLdapImpl();
	
	private JwtConsumer jwtConsumer = new JwtConsumer();
	private JwtProducer jwtProducer = new JwtProducer();
	
	@Override
	public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {
		
		String userdn = null;
		
		// extract username and password from encoded HTTP 'Authorization' header
		// header value format will be "Basic encodedstring" for Basic authentication.
		// Example "Basic YWRtaW46YWRtaW4="
		String authCredentials = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
		LOG.info("Establishing authentication: HTTP header="+authCredentials);
		
		if (authCredentials==null) {
			
			// 401: authentication required
			// custom exception to send the "WWW-Authenticate" header and trigger client challenge
			throw new MissingAuthenticationHeaderException(Status.UNAUTHORIZED);
			
		// HTTP Basic Authentication
		// Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
		} else if (authCredentials.indexOf("Basic")>=0) {
			
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
				
				// create Jason Web Token
				String token = jwtProducer.getToken(userdn);
				LOG.info("Generated token = "+token);
				containerRequest.setProperty(JWT, token);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		// Jason Web Token
		// Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm.eFONFh7HgQ
		} else if (authCredentials.indexOf("Bearer")>=0) {
			
			try {
				
				final String token = authCredentials.replaceFirst("Bearer", "").trim();
				LOG.info("Retrieved JWT="+token);
				
				DecodedJWT jwt = jwtConsumer.verifyToken(token);
				userdn = jwt.getSubject();
				LOG.info("Retrieved user DN = "+userdn);
			
			} catch(JWTVerificationException e) {
				LOG.warning("Detected an invalid JWT token");
				e.printStackTrace();
			}
			
		}

		if (userdn==null) {
		
			// 403: not authorized
			throw new WebApplicationException(Status.FORBIDDEN);
			
		} else {
			
			containerRequest.setProperty(USER_DN, userdn);
			
			List<String> ugroups = userService.getUserGroups(userdn);
			containerRequest.setProperty(USER_GROUPS_PROPERTY, ugroups);
			LOG.info("Storing in request user dn: "+userdn+", user groups: "+ugroups);
			// then proceed with normal filter chain
			
		}
		
	}
}