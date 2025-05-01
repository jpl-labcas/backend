package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MultivaluedHashMap;

import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

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
	public final static String GUEST_USER_DN = "uid=guest,ou=public";
	
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
		//
		// But if POST'ed, then the username and password are url-form-encoded 
		String authCredentials = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
		LOG.info("Establishing authentication: HTTP header="+authCredentials);
		
		// or extract information from "JasonWebToken" cookie
		String cookie = getCookie(containerRequest, JWT);
		LOG.info("Establishing authentication: '"+JWT+"' cookie="+cookie);
		
		if (authCredentials==null) {
			LOG.info("👉 authCredentials is null");
			if (cookie!=null) {
				LOG.info("👉 cookie is not null");
				DecodedJWT jwt = jwtConsumer.verifyToken(cookie);
				userdn = jwt.getSubject();
				LOG.info("Retrieved user DN = "+userdn);
				
			} else {
				LOG.info("👉 checking for POST'ed creds");
				// Check for POST'ed credentials
				try {
					InputStream entityStream = containerRequest.getEntityStream();
					String body = new Scanner(entityStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
					MultivaluedMap<String, String> formParams = parseFormParams(body);
					String username = formParams.getFirst("username");
					String password = formParams.getFirst("password");
					containerRequest.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

					LOG.info("👉 Possible POST username «" + username + "»");

					userdn = userService.getValidUser(username, password);
					LOG.info("Retrieved user DN = "+userdn);
					// create Jason Web Token?
					String uriPath = containerRequest.getUriInfo().getPath();
					LOG.info("URI path="+uriPath);
					if (uriPath.contains("auth")) {
						String token = jwtProducer.getToken(userdn);
						LOG.info("Generated token1 = "+token);
						containerRequest.setProperty(JWT, token);
					}
				} catch (NoSuchElementException ex) {
					LOG.info("🕵️ No creds given, so grant guest access only");
					containerRequest.setProperty(USER_DN, GUEST_USER_DN);
					// empty group list
					containerRequest.setProperty(USER_GROUPS_PROPERTY, new ArrayList<String>());
					return;
				} catch (RuntimeException ex) {
					LOG.warning("🚨 how did we get here? exception " + ex);
					throw ex;
				} catch (Exception ex) {
					LOG.info("🤷 Can't figure out creds, so grant guest access only");
					containerRequest.setProperty(USER_DN, GUEST_USER_DN);
					// empty group list
					containerRequest.setProperty(USER_GROUPS_PROPERTY, new ArrayList<String>());
					return;
				}
				// 401: authentication required
				// custom exception to send the "WWW-Authenticate" header and trigger client challenge
				//throw new MissingAuthenticationHeaderException(Status.UNAUTHORIZED);
				
			}
			
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
				
				// create Jason Web Token?
				String uriPath = containerRequest.getUriInfo().getPath();
				LOG.info("URI path="+uriPath);
				if (uriPath.contains("auth")) {
					String token = jwtProducer.getToken(userdn);
					LOG.info("Generated token2 = "+token);
					containerRequest.setProperty(JWT, token);
				}
				
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
				LOG.info("🧐🧐🧐 Retrieved user DN = " + userdn);

				Date modified = userService.getModificationTime(userdn);
				Date issued = jwt.getIssuedAt();

				if (modified.after(issued)) {
					LOG.info("🚨‼️🚨‼️ user " + userdn + " LDAP record was modified on " + modified
						+ " which is later than JWT issue date of " + issued + " so this JWT is no longer valid"
					);
					throw new WebApplicationException(Status.UNAUTHORIZED);
				}
			
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
	
	private String getCookie(ContainerRequestContext containerRequest, String cookieName) {
		
		Map<String, Cookie> cookies = containerRequest.getCookies();
		if (cookies.containsKey(cookieName)) {
			return cookies.get(cookieName).getValue();
		} else {
			return null;
		}
	}

	private MultivaluedMap<String, String> parseFormParams(String body) {
		try {
			MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
			String[] params = body.split("&");
			for (String param: params) {
				String[] keyValue = param.split("=");
				String key = URLDecoder.decode(keyValue[0], "UTF-8");
				String value = keyValue.length > 1? URLDecoder.decode(keyValue[1], "UTF-8") : "";
				formParams.add(key, value);
			}
			return formParams;
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
			throw new IllegalStateException("Unexpected UnsupportedEncodingException, aborting", ex);
		}
	}
}
