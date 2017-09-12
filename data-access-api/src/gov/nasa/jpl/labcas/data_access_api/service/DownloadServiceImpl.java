package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class DownloadServiceImpl extends SolrProxy implements DownloadService  {
	
	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());
	
	// read from ~/labcas.properties
	private final static String DATA_ACCESS_API_BASE_URL_PROPERTY = "dataAccessApiBaseUrl";
	protected String dataAccessApiBaseUrl = null;

	public DownloadServiceImpl() {
		
		this.dataAccessApiBaseUrl = Parameters.getParameterValue(DATA_ACCESS_API_BASE_URL_PROPERTY);
		
	}

	@Override
	@GET
	@Path("/collections/download")
	public Response downloadCollections(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {
		
		// build Solr query to 'collections' core
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows);

		// execute Solr query to 'collections' core
		// extract matching collection ids
		List<String> collectionIds = new ArrayList<String>();
		try {

			LOG.info("Executing Solr request to 'collections' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_COLLECTIONS).query(request);
			this.extractIds(response, collectionIds);

		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		// query for all files matching these collection ids
		if (collectionIds.size() > 0) {
			return executeFilesQuery(SOLR_FIELD_COLLECTION_ID, collectionIds);
		} else {
			return Response.status(200).entity("").build();
		}
		
	}

	@Override
	@GET
	@Path("/datasets/download")
	public Response downloadDatasets(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {

		// build Solr query to 'datasets' core
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows);

		// execute Solr query to 'datasets' core
		// extract matching dataset ids
		List<String> datasetIds = new ArrayList<String>();
		try {

			LOG.info("Executing Solr request to 'datasets' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_DATASETS).query(request);
			this.extractIds(response, datasetIds);

		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		// query for all files matching these dataset ids
		if (datasetIds.size() > 0) {
			return executeFilesQuery(SOLR_FIELD_DATASET_ID, datasetIds);
		} else {
			return Response.status(200).entity("").build();
		}


	}

	@Override
	@GET
	@Path("/files/download")
	public Response downloadFiles(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows) {

		// build Solr query
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows); 

		// execute Solr query to 'files' core, build result document
		String results = "";
		try {

			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			results = buildResultsDocument(response);
			
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		return Response.status(200).entity(results).build();

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
	
			return Response.status(200).entity(results).build();
			
		} catch(Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
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
			results += this.dataAccessApiBaseUrl + "?id=" + id + "\n";
		}
		
		return results;
		
	}

}
