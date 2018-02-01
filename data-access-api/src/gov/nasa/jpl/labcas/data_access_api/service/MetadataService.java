package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;


/**
 * Service use to update the LabCAS metadata stored in the Solr index
 * 
 * @author Luca Cinquini
 *
 */
public interface MetadataService {
	
	/**
	 * Method to update a single metadata record
	 * through an HTTP/GET request.
	 * @param core
	 * @param action
	 * @param id
	 * @param field
	 * @param values
	 * @return
	 */
	public Response updateById(HttpServletRequest httpRequest, ContainerRequestContext requestContext,
			                   String core, String action, String id, String field, List<String> values);
	
	/**
	 * Method to bulk update multiple metadata records at once
	 * through an HTTP/POST request with XML payload.
	 * 
	 * @param document
	 * @return
	 */
	public Response update(HttpServletRequest httpRequest, ContainerRequestContext requestContext);

}
