package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Service used to issue and refresh Jason Web Tokens.
 * 
 * @author Luca Cinquini
 *
 */
public interface TokenService {
	
	/**
	 * Method to issue a new token
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @param sort
	 * @return
	 */
	public Response auth(HttpServletRequest httpRequest, ContainerRequestContext requestContext);
	
}
