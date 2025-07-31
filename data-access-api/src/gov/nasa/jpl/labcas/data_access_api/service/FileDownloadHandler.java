package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import gov.nasa.jpl.labcas.data_access_api.aws.AwsS3DownloadHelper;
import gov.nasa.jpl.labcas.data_access_api.aws.AwsUtils;
import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;
import gov.nasa.jpl.labcas.data_access_api.utils.DownloadHelper;

/**
 * Handles the actual file downloading logic.
 */
public class FileDownloadHandler extends SolrProxy {
    
    private static final Logger LOG = Logger.getLogger(FileDownloadHandler.class.getName());
    
    private final AwsS3DownloadHelper awsS3DownloadHelper;
    private final FilePathResolver filePathResolver;
    
    public FileDownloadHandler() throws Exception {
        super();
        this.awsS3DownloadHelper = new AwsS3DownloadHelper();
        this.filePathResolver = new FilePathResolver();
    }
    
    /**
     * Handles downloading a file by ID.
     * 
     * @param requestContext The request context for authentication and access control
     * @param id The file ID to download
     * @param suppressContentDisposition Whether to suppress the content disposition header
     * @return Response object with the file or error
     */
    public Response downloadFile(javax.ws.rs.container.ContainerRequestContext requestContext, 
                               String id, 
                               boolean suppressContentDisposition) {
        LOG.info("📯 HEYO! I am in the download part");

        // VDP_1645_SC-9999-L-JPL-0220 — ensure credentials are always provided
        String distinguishedName = (String) requestContext.getProperty(AuthenticationFilter.USER_DN);
        LOG.info("🪪 the distinguishedName is «" + distinguishedName + "»");
        if (distinguishedName == null || distinguishedName.equals(AuthenticationFilter.GUEST_USER_DN)) {
            LOG.info("VDP_1645_SC-9999-L-JPL-0220 violation: login required to download (even for public data)");
            return Response.status(Status.UNAUTHORIZED)
                .entity("User login required to download (even for public data)").build();
        }

        // note: @QueryParam('id') automatically URL-decodes the 'id' value
        if (id == null) {
            return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
        } else if (!isSafe(id)) {
            return Response.status(Status.BAD_REQUEST).entity(UNSAFE_CHARACTERS_MESSAGE).build();
        }

        try {
            FilePathResolver.FileInfo fileInfo = filePathResolver.getFileInfo(requestContext, id);
            
            if (fileInfo != null) {
                String fileLocation = fileInfo.getFileLocation();
                String fileName = fileInfo.getFileName();
                String realFileName = fileInfo.getRealFileName();
                String filePath = fileInfo.getFilePath();
                
                LOG.info("Using fileLocation=" + fileLocation);
                
                if (fileLocation.startsWith("s3")) {
                    // generate temporary URL and redirect client
                    String key = AwsUtils.getS3key(filePath);
                    LOG.info("Using s3key=" + key);
                    URL url = awsS3DownloadHelper.getUrl(key, null); // versionId=null
                    LOG.info("Redirecting client to S3 URL:" + url.toString());
                    return Response.temporaryRedirect(url.toURI()).build();
                    
                } else {
                    // Download audit track
                    DownloadAuditLogger.logDownload(distinguishedName, id);

                    // read file from local file system and stream it to client
                    DownloadHelper dh = new DownloadHelper(Paths.get(filePath));
                    long size = dh.getFileSize();
                    String lowercase = filePath.toLowerCase();
                    String mediaType = lowercase.endsWith(".dcm") || lowercase.contains("dicom")?
                        "application/dicom" : MediaType.APPLICATION_OCTET_STREAM;
                    LOG.info("😒 using mediaType = " + mediaType);
                    if (suppressContentDisposition) {
                       return Response
                           .ok(dh, mediaType)
                           .header("Content-length", size)
                           .build();
                    } else {
                       return Response
                           .ok(dh, mediaType)
                           .header("Content-length", size)
                           // "Content-disposition" header instructs the client to keep the same file name
                           .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                           .build();
                    }
                }
            } else {
                return Response.status(Status.NOT_FOUND).entity(
                    "🤢 File not found or not authorized"
                ).build();
            }
        } catch (RuntimeException e) {
            LOG.info("=== RUNTIME EXCEPTION " + e.getClass().getName());
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