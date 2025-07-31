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

@Path("/")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class DownloadServiceImpl implements DownloadService  {
	
	// Initialize SSL configuration
	static {
		SslConfigurator.configureSsl();
	}

	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());
	
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
						LOG.info("👀🔍🎉 success! Adding file " + f + " to files");
						files.add(f);
					} else {
						LOG.warning("👀🔍💥 file ID " + fileID + " not found");
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
