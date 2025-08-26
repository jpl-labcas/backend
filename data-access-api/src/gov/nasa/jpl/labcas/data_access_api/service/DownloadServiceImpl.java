package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.json.JSONObject;

@Path("/")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class DownloadServiceImpl implements DownloadService  {
	
	// Initialize SSL configuration
	static {
		SslConfigurator.configureSsl();
	}

	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());

	/**
	 * This is the S3 URI for the public collections for Aspera transfers.
	 *
	 * Apparently this should be the S3 URI but without the `s3://edrn-labcas/uploads` prefix.
	 * Don't forget to add the trailing slash.
	 */
	private static final String S3_PUBLIC_COLLECTIONS = "/workspaces/107571/home/edrn-bot@jpl.nasa.gov (1251184)/";
	
	/**
	 * This is the remote host for Aspera transfers: `ats-aws-us-west-2.aspera.io`.
	 */
	private static final String ASPERA_REMOTE_HOST = "ats-aws-us-west-2.aspera.io";

	/**
	 * Remote user for Aspera transfers: `xfer`.
	 */
	private static final String ASPERA_REMOTE_USER = "xfer";

	/**
	 * Transfer direction for Aspera transfers: `receive`.
	 */
	private static final String ASPERA_TRANSFER_DIRECTION = "receive";

	/**
	 * SSH port for Aspera transfers: `33001`.
	 */
	private static final int ASPERA_SSH_PORT = 33001;

	private final FileDownloadHandler fileDownloadHandler;
	private final FilePathResolver filePathResolver;
	
	public DownloadServiceImpl() throws Exception {
		this.fileDownloadHandler = new FileDownloadHandler();
		this.filePathResolver = new FilePathResolver();
	}

	@Override
	@GET
	@Path("/download")
	@Produces({CustomMediaTypes.APPLICATION_DICOM, MediaType.APPLICATION_OCTET_STREAM})
	public Response download(
		@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
		@QueryParam("id") String id,
		@QueryParam("suppressContentDisposition") @DefaultValue("false") boolean suppressContentDisposition) {
		
		return fileDownloadHandler.downloadFile(requestContext, id, suppressContentDisposition);
	}


	@Override
	@POST
	@Path("/zip")
	@Produces("text/plain")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response zip(
		@Context HttpServletRequest httpRequest,
		@Context ContainerRequestContext requestContext,
		@FormParam("email") String email,
		@FormParam("query") @DefaultValue("") String query,
		@FormParam("id") List<String> ids
	) {
		LOG.info("👀 I see you, " + email + ", with your zip request for query «" + query + "» or for "
			+ ids.size() + " files with IDs «" + ids + "»");
		try {
			List<String> files = null;
			if (query.length() > 0) {
				LOG.info("👀 The query length was > 0 so resolving using " + query);
				files = filePathResolver.getFilePathsForQuery(requestContext, query);
				LOG.info("👀 For query " + query + " the file IDs are " + files);
			} else {
				files = new ArrayList<String>();
				for (String fileID: ids) {
					LOG.info("👀🔎 resolving file ID " + fileID);
					String f = filePathResolver.getFile(requestContext, fileID);
					LOG.info("👀🔎 resolved file ID " + fileID + " to " + f);
					if (f != null) {
						files.add(f);
					} else {
						LOG.warning("🚨🚨🚨 file ID " + fileID + " not found");
					}
				}
			}

			LOG.info("👀 the files are: " + files);
			if (files.isEmpty())
				return null;  // Should give 204 no content

			String uuid = ZipperlabService.initiateZIP(email, files);
			LOG.info("👀 uuid is " + uuid);
			return Response.status(Status.OK).entity(uuid).build();
		} catch (Exception ex) {
			LOG.warning("🚨🚨🚨 " + ex.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	@GET
	@Path("/rapidly-download-collection")
	@Produces("application/json")
	public Response rapidlyDownloadCollection(
		@Context HttpServletRequest httpRequest,
		@Context ContainerRequestContext requestContext,
		@QueryParam("collectionID") String collectionID,
		@QueryParam("token") String token
	) {
		LOG.info("🏎️ I see you, with your Aspera request for collection " + collectionID + " with token «" + token + "»");

		String path = S3_PUBLIC_COLLECTIONS + collectionID;

		// Create JSON response with nested structure
		// See https://github.com/jpl-labcas/backend/issues/12 for details of this JSON structure
		// and https://github.com/jpl-labcas/backend/issues/20 for the task.
		JSONObject jsonResponse = new JSONObject();
		JSONObject transferRequest = new JSONObject();
		JSONObject source = new JSONObject();
		source.put("source", path);
		JSONObject singleRequest = new JSONObject();
		singleRequest.put("paths", new JSONObject[]{source});
		singleRequest.put("authentication", token);
		singleRequest.put("remote_host", ASPERA_REMOTE_HOST);
		singleRequest.put("remote_user", ASPERA_REMOTE_USER);
		singleRequest.put("direction", ASPERA_TRANSFER_DIRECTION);
		singleRequest.put("ssh_port", ASPERA_SSH_PORT);
		transferRequest.put("transfer_request", singleRequest);

		JSONObject[] transferRequestsArray = new JSONObject[]{transferRequest};
		jsonResponse.put("transfer_requests", transferRequestsArray);

		return Response.status(Status.OK).entity(jsonResponse.toString()).build();
	}

	@Override
	@GET
	@Path("/ping")
	@Produces("text/plain")
	public Response ping(
		@Context HttpServletRequest httpRequest,
		@Context ContainerRequestContext requestContext,
		@QueryParam("message") String message
	) {
		LOG.info("📡 Message received: " + message);
		return Response.status(Status.OK).entity("🧾 Message received, " + message + "\n").build();
	}
}
