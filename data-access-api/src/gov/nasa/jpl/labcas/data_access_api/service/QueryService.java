package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * Service to issue a query request to Solr.
 * 
 * @author Luca Cinquini
 *
 */
public interface QueryService {
	
	/**
	 * Method to query Solr for matching collections.
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @return
	 */
	public Response queryCollections(HttpServletRequest httpRequest, String q, List<String> fq, int start, int rows);
	
	/**
	 * Method to query Solr for matching datasets.
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @return
	 */
	public Response queryDatasets(HttpServletRequest httpRequest, String q, List<String> fq, int start, int rows);
	
	/**
	 * Method to query Solr for matching files.
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @return
	 */
	public Response queryFiles(HttpServletRequest httpRequest, String q, List<String> fq, int start, int rows);


}
