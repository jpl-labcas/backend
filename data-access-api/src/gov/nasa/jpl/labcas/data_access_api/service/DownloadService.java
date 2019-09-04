package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

public interface DownloadService {
	
	/**
	 * Method to download a file given its id.
	 * 
	 * @param httpRequest
	 * @param requestContext
	 * @param id
	 * @return
	 */
	public Response download(HttpServletRequest httpRequest, ContainerRequestContext requestContext, String id);
	
}