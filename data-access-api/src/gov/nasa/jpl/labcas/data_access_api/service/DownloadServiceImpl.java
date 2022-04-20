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
		throw new RuntimeException("😆 HEYO forget this I am outta here!");

	}
	
}
