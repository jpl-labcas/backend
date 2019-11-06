package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import gov.nasa.jpl.labcas.data_access_api.exceptions.UnsafeCharactersException;
import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;
import gov.nasa.jpl.labcas.data_access_api.utils.UrlUtils;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class ListServiceImpl extends SolrProxy implements ListService  {
	
	private final static Logger LOG = Logger.getLogger(ListServiceImpl.class.getName());
	
	// read from ~/labcas.properties
	private final static String DATA_ACCESS_API_BASE_URL_PROPERTY = "dataAccessApiBaseUrl";
	protected String dataAccessApiBaseUrl = null;

	public ListServiceImpl() {
		
		this.dataAccessApiBaseUrl = Parameters.getParameterValue(DATA_ACCESS_API_BASE_URL_PROPERTY);
		
	}

	@Override
	@GET
	@Path("/collections/list")
	public Response listCollections(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
			@QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {
				
		// execute Solr query to 'collections' core
		// extract matching collection ids
		List<String> collectionIds = new ArrayList<String>();
		try {
			
			// build Solr query to 'collections' core
			SolrQuery request = this.buildPassThroughQuery(httpRequest, requestContext, q, fq, start, rows);

			LOG.info("Executing Solr request to 'collections' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_COLLECTIONS).query(request);
			this.extractIds(response, collectionIds);

		} catch(UnsafeCharactersException unsafe) {
			return Response.status(Status.BAD_REQUEST).entity(unsafe.getMessage()).build();
			
		} catch (Exception e) {
			// send Status.INTERNAL_SERVER_ERROR "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		// query for all files matching these collection ids
		if (collectionIds.size() > 0) {
			return executeFilesQuery(SOLR_FIELD_COLLECTION_ID, collectionIds);
		} else {
			return Response.status(Status.OK).entity("").build();
		}
		
	}

	@Override
	@GET
	@Path("/datasets/list")
	public Response listDatasets(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
			@QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {

		// execute Solr query to 'datasets' core
		// extract matching dataset ids
		List<String> datasetIds = new ArrayList<String>();
		try {
			
			// build Solr query to 'datasets' core
			SolrQuery request = this.buildPassThroughQuery(httpRequest, requestContext, q, fq, start, rows);

			LOG.info("Executing Solr request to 'datasets' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_DATASETS).query(request);
			this.extractIds(response, datasetIds);
			
		} catch(UnsafeCharactersException unsafe) {
			return Response.status(Status.BAD_REQUEST).entity(unsafe.getMessage()).build();

		} catch (Exception e) {
			// send Status.INTERNAL_SERVER_ERROR "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		// query for all files matching these dataset ids
		if (datasetIds.size() > 0) {
			return executeFilesQuery(SOLR_FIELD_DATASET_ID, datasetIds);
		} else {
			return Response.status(Status.OK).entity("").build();
		}


	}

	@Override
	@GET
	@Path("/files/list")
	public Response listFiles(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
			@QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {


		// execute Solr query to 'files' core, build result document
		String results = "";
		try {
			
			// build Solr query
			SolrQuery request = this.buildPassThroughQuery(httpRequest, requestContext, q, fq, start, rows); 

			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			results = buildResultsDocument(response);
			
		} catch(UnsafeCharactersException unsafe) {
			return Response.status(Status.BAD_REQUEST).entity(unsafe.getMessage()).build();
			
		} catch (Exception e) {
			// send Status.INTERNAL_SERVER_ERROR "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		return Response.status(Status.OK).entity(results).build();

	}
	
	
	/**
	 * Method that converts a query request to this service to a query request to the Solr server (for any core).
	 * Access control constraints are added from the security groups retrieved by the upstream authentication filter.
	 * 
	 * @param httpRequest
	 * @param q
	 * @param fq
	 * @param start
	 * @param rows
	 * @param sort
	 * @param fields
	 * @return
	 */
	private SolrQuery buildPassThroughQuery(final HttpServletRequest httpRequest, 
			                                  final ContainerRequestContext requestContext,
			                                  final String q, final List<String> fq,
			                                  final int start, final int rows) throws Exception {
		
		// note: the arguments 'q', 'fq', 'start', 'rows' 
		// have already been URL-decoded by the upstream methods
		LOG.info("HTTP request URL=" + httpRequest.getRequestURL());
		LOG.info("HTTP request query string=" + httpRequest.getQueryString());
		
		// check user input for unsafe characters
		if (!isSafe(q) || !isSafe(fq)) {
			LOG.warning("Detected UNSAFE CHARACTERS in request");
			throw new UnsafeCharactersException(UNSAFE_CHARACTERS_MESSAGE, Status.BAD_REQUEST);
		}

		// enforce access control by adding OwnerPrincipal constraint
		String acfq = getAccessControlQueryStringValue(requestContext);
		if (!acfq.isEmpty()) {
			fq.add(acfq);
		}

		// build Solr query
		// parameter value encoding will be executed by SolrJ
		SolrQuery request = new SolrQuery();
		if (q != null && !q.isEmpty()) {
			request.setQuery(q);
		}
		if (fq != null && fq.size() > 0) {
			request.setFilterQueries(fq.toArray(new String[fq.size()]));
		}
		if (start > 0) {
			request.setStart(start);
		}
		if (rows > 0) {
			request.setRows(rows);
		}
				
		// return sorted "id" field
		request.setFields( new String[] { SOLR_FIELD_ID } );
		// always sort by result "id"
		request.setSortField(SOLR_FIELD_ID, ORDER.desc);
		
		return request;

	}

	
	
	/**
	 * Method that executes a query for all files matching a set of DatasetIds or CollectionIds, 
	 * until all are returned.
	 * 
	 * @return
	 * @throws Exception
	 */
	private Response executeFilesQuery(final String idKey, final List<String> idValues) {
		
		try {
			
			String results = "";
			SolrQuery request = this.buildFilesQuery(idKey, idValues);
			int count = 0;
			long numFound = 1;
			while (count < numFound) {
				
				request.setStart(count);
				LOG.info("Executing Solr request to 'files' core: "+request.toString());
				QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
				
				SolrDocumentList docList =  response.getResults();
				numFound = docList.getNumFound();
				count += docList.size();
				LOG.info("...number of results found: "+numFound);
				
				// keep adding results to the same document
				results += buildResultsDocument(response);
				
			}
	
			return Response.status(Status.OK).entity(results).build();
			
		} catch(Exception e) {
			// send Status.INTERNAL_SERVER_ERROR "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
	}
	
	/**
	 * Method that builds a query for files matching a set of DatasetIds or CollectionIds.
	 * 
	 * Example: q=DatasetId%3A%28%22MD_Anderson_Pancreas_IPMN_images.IPMN_P1-06_H03%22%29&fl=id%2CFileDownloadId&rows=10000
	 * 
	 * @return
	 */
	private SolrQuery buildFilesQuery(final String idKey, final List<String> idValues) {
		
		SolrQuery request = new SolrQuery();
		String idquery = idKey + ":(";
		for (int i = 0; i < idValues.size(); i++) {
			if (i > 0) {
				idquery += " OR ";
			}
			idquery += "\"" + idValues.get(i) + "\"";
		}
		idquery += ")";
		request.setQuery(idquery);
		// add fields to be returned
		request.setFields( new String[] { SOLR_FIELD_ID } );
		request.setRows(SOLR_MAX_NUM_FILES);
		
		return request;
		
	}
	
	/**
	 * Method that parses a Solr response to extract the result ids.
	 * @param response
	 * @param ids
	 */
	private void extractIds(final QueryResponse response, List<String> ids) {
		SolrDocumentList docs = response.getResults();
		Iterator<SolrDocument> iter = docs.iterator();
		while (iter.hasNext()) {
			SolrDocument doc = iter.next();
			LOG.info(doc.toString());
			String id = (String) doc.getFieldValue(SOLR_FIELD_ID);
			ids.add(id);
		}
	}
	
	/**
	 * Method that parses a Solr response from the 'files' core and builds a list of files download URLs.
	 * 
	 * @return
	 */
	private String buildResultsDocument(final QueryResponse response) {
		
		String results = "";
		SolrDocumentList docs = response.getResults();
		Iterator<SolrDocument> iter = docs.iterator();
		while (iter.hasNext()) {
			SolrDocument doc = iter.next();
			LOG.fine(doc.toString());
			String id = (String) doc.getFieldValue(SOLR_FIELD_ID);
			try {
				// example:  https://edrn-labcas.jpl.nasa.gov/data-access-api/download?id=Automated_Quantitative_Measures_of_Breast_Density_Data%2FN0580%2FRAW%2FN0580_MG_DAT_RCC.dcm
				// note: spaces are URL-encoded as '+' but they should really be '%20'
				results += this.dataAccessApiBaseUrl + UrlUtils.encode(id) + "\n";
			} catch(UnsupportedEncodingException e) {
				// do not include thie file in the list of results
				LOG.warning("Error encoding download URL for file id="+id);
				LOG.warning(e.getMessage());
			}
		}
		
		return results;
		
	}

}
