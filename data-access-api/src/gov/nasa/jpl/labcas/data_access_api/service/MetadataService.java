package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Service used to update the LabCAS metadata stored in the Solr index.
 */
public interface MetadataService {
	
	/**
	 * Method to perform a single operation on one or more records identified by id.
	 * @param cores
	 * @param action
	 * @param id
	 * @param field
	 * @param values
	 * @return
	 */
	public Response updateById(HttpServletRequest httpRequest, ContainerRequestContext requestContext, HttpHeaders headers,
			                   List<String> cores, String action, String id, String field, List<String> values);
	
	/**
	 * Method to perform multiple operations on different classes of records
	 * identified by a query specification.
	 * 
	 * @param document
	 * @return
	 */
	public Response update(HttpServletRequest httpRequest, ContainerRequestContext requestContext, HttpHeaders headers, String content);

}
