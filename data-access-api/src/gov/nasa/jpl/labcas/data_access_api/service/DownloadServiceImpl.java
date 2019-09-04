package gov.nasa.jpl.labcas.data_access_api.service;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

@Path("/")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class DownloadServiceImpl extends SolrProxy implements DownloadService  {
	
	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());

	@Override
	@GET
	@Path("/download")
	public Response download(HttpServletRequest httpRequest, ContainerRequestContext requestContext, @QueryParam("id") String id) {
		
		try {
			
			// query Solr for file with that specific id
			SolrQuery request = new SolrQuery();
			request.setQuery("id:"+id);
			
			// add access control
			String acfq = getAccessControlQueryStringValue(requestContext);
			if (!acfq.isEmpty()) {
				request.setFilterQueries(acfq);
			}
			
			// return file location on file system or S3 + file name
			request.setFields( new String[] { SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME } );
			
			LOG.info("Executing Solr request to 'files' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			
			SolrDocumentList docs = response.getResults();
			Iterator<SolrDocument> iter = docs.iterator();
			while (iter.hasNext()) {
				SolrDocument doc = iter.next();
				LOG.info(doc.toString());
				String fileLocation = (String)doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
				String fileName = (String)doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				java.nio.file.Path filePath = Paths.get(fileLocation, fileName);
				LOG.info("File path="+filePath.toString());
				
				return Response.status(200).entity(filePath.toString()).build();
				
			}
			
			
			
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(500).entity(e.getMessage()).build();
		}
		
		return Response.status(200).entity("").build();

	}
	
}