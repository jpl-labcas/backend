package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Iterator;
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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import gov.nasa.jpl.labcas.data_access_api.aws.AwsS3DownloadHelper;
import gov.nasa.jpl.labcas.data_access_api.aws.AwsUtils;
import gov.nasa.jpl.labcas.data_access_api.utils.DownloadHelper;

@Path("/")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class DownloadServiceImpl extends SolrProxy implements DownloadService  {
	
	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());
	
	// class that generates temporary URLs for S3 downloads
	AwsS3DownloadHelper awsS3DownloadHelper;
	
	public DownloadServiceImpl() throws Exception {
		
		super();
		
		awsS3DownloadHelper = new AwsS3DownloadHelper();
		
	}

	@Override
	@GET
	@Path("/download")
	public Response download(@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext, @QueryParam("id") String id) {
		LOG.warning("📯 HEYO! I am in the download part");

		// note: @QueryParam('id') automatically URL-decodes the 'id' value
		if (id==null) {
			return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
		} else if (!isSafe(id)) {
			return Response.status(Status.BAD_REQUEST).entity(UNSAFE_CHARACTERS_MESSAGE).build();
		}

		try {
			
			String fileLocation = null;
			String fileName = null;
			String filePath = null;
			String name = null;
			
			// query Solr for file with that specific id
			SolrQuery request = new SolrQuery();
			request.setQuery("id:\""+id+"\"");
			LOG.warning("🆔 HEYO! The id is «" + id + "»");
			
			// add access control
			String acfq = getAccessControlQueryStringValue(requestContext);
			if (!acfq.isEmpty()) {
				request.setFilterQueries(acfq);
			}
			
			// return file location on file system or S3 + file name
			request.setFields( new String[] { SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME, SOLR_FIELD_NAME } );
			
			// note: SolrJ will URL-encode the HTTP GET parameter values
			LOG.info("Executing Solr request to 'files' core: "+request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			
			SolrDocumentList docs = response.getResults();
			Iterator<SolrDocument> iter = docs.iterator();
			while (iter.hasNext()) {
				SolrDocument doc = iter.next();
				LOG.info(doc.toString());
				LOG.warning("=== 1 about to get fileLocation");
				fileLocation = (String)doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
				LOG.warning("=== 2 got fileLocation = «" + fileLocation + "»");
				fileName = (String)doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				LOG.warning("=== 3 got fileName = «" + fileName + "»");
				LOG.warning("=== 3½ the class of «" + SOLR_FIELD_NAME + "» in the doc is «" + doc.getFieldValue(SOLR_FIELD_NAME).getClass().getName() + "»");
				name=(String)doc.getFieldValue(SOLR_FIELD_NAME);
				LOG.warning("=== 4 got name = «" + name + "»");
				LOG.warning("HEYO 🚨 For id «" + id + "» and field «" + SOLR_FIELD_NAME + "» I got «" + name + "»");
				if (name!=null) {
					LOG.warning("=== 5 since name is not null, setting fileName to «" + name + "»");
					fileName=name;
				}
				filePath = fileLocation + "/" + fileName;
				LOG.warning("=== 6 filePath is «" + filePath + "»");
				LOG.info("File path="+filePath.toString());
				
				//return Response.status(Status.OK).entity(filePath.toString()).build();
				
			}
						
			if (filePath!=null) {

				LOG.info("Using fileLocation="+fileLocation);
				
				if (fileLocation.startsWith("s3")) {
					
					// generate temporary URL and redirect client
					String key = AwsUtils.getS3key(filePath);
					LOG.info("Using s3key="+key);
					URL url = awsS3DownloadHelper.getUrl(key, null); // versionId=null
					LOG.info("Redirecting client to S3 URL:"+url.toString());
					return Response.temporaryRedirect(url.toURI()).build();
					
				} else {
				
					// read file from local file system and stream it to client
					DownloadHelper dh = new DownloadHelper(Paths.get(filePath));
			        return Response
			                .ok(dh, MediaType.APPLICATION_OCTET_STREAM)
			                // "content-disposition" header instructs the client to keep the same file name
			                .header("content-disposition","attachment; filename=\"" + fileName + "\"")
			                .build();
				}
	        
			} else {
				return Response.status(Status.NOT_FOUND).entity("File not found or not authorized").build();
			}	
		} catch (RuntimeException e) {
			LOG.warning("=== RUNTIME EXCEPTION " + e.getClass().getName());
			throw e;
		} catch (Exception e) {
			// send 500 "Internal Server Error" response
			LOG.warning("💥 HEYO … nope! We got an exception of type «" + e.getClass().getName() + "»");
			e.printStackTrace();
			LOG.warning(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}
	
}
