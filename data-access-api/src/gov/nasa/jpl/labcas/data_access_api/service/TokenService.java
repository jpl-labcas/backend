package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Service used to issue and refresh JSON Web Tokens.
 * 
 */
public interface TokenService {
	
	/**
	 * Method to issue a new token
	 * 
	 * @param httpRequest
	 * @param requestContext
	 * @return
	 */
	public Response auth(HttpServletRequest httpRequest, ContainerRequestContext requestContext);

	/**
	 * Method to log out a session
	 * 
	 * @param httpRequest
	 * @param requestContext
	 * @param sessionID
	 * @return
	 */
	public Response logout(HttpServletRequest httpRequest, ContainerRequestContext requestContext, String sessionID);
	
}
