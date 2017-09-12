package gov.nasa.jpl.labcas.data_access_api.service;

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
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;

/**
 * Service implementation to issue a query request to Solr.
 * 
 * @author Luca Cinquini
 *
 */
@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class QueryServiceImpl extends SolrProxy implements QueryService {
	
	private final static Logger LOG = Logger.getLogger(QueryServiceImpl.class.getName());

	public QueryServiceImpl() {
		super();
	}

	@Override
	public Response queryCollections(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows,
			@QueryParam("sort") String sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response queryDatasets(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows,
			@QueryParam("sort") String sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@GET
	@Path("/files/select")
	public Response queryFiles(@Context HttpServletRequest httpRequest, @QueryParam("q") String q,
			@QueryParam("fq") List<String> fq, @QueryParam("start") int start, @QueryParam("rows") int rows, 
			@QueryParam("sort") String sort) {
		
		// build Solr query
		SolrQuery request = this.buildPassThroughQuery(httpRequest, q, fq, start, rows, sort); 

		// execute Solr query to 'files' core, build result document
		String results = "";
		try {

			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			System.out.println("SOLR RESPONSE="+response.toString());
			SolrDocumentList docs = response.getResults();
			for (int i = 0; i < docs.size(); i++) {
		        String xml = ClientUtils.toXML(ClientUtils.toSolrInputDocument(docs.get(i)));
		        results += xml;
		    }
			
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}

		return Response.status(200).entity(results).build();
		
		
	}

}
