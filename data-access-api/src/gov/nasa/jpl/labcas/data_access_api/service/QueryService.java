package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * Service to proxy a query request to Solr.
 * 
 * @author Luca Cinquini
 *
 */
public interface QueryService {

	/**
	 * Method to query Solr for matching collections.
	 * 
	 * @param httpRequest
	 * 
	 * @return
	 */
	public Response queryCollections(HttpServletRequest httpRequest);

	/**
	 * Method to query Solr for matching datasets.
	 * 
	 * @param httpRequest
	 * 
	 * @return
	 */
	public Response queryDatasets(HttpServletRequest httpRequest);

	/**
	 * Method to query Solr for matching files.
	 * 
	 * @param httpRequest
	 * 
	 * @return
	 */
	public Response queryFiles(HttpServletRequest httpRequest);

}
