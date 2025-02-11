package gov.nasa.jpl.labcas.data_access_api.service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

// Download audit track
import gov.nasa.jpl.labcas.data_access_api.filter.AuthenticationFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
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

// Zipperlab
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;
import org.apache.solr.client.solrj.SolrServerException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

// David's OHIF viewer
import javax.ws.rs.Produces;


@Path("/")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class DownloadServiceImpl extends SolrProxy implements DownloadService  {
	
	static {
		// Because our sysadmins require us to use HTTPS between systems, and services
		// on edrn-docker use self-signed certs, we need to trust all certs. Is this really 
		// more secure? 🙄
		try {
			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				}
			};
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) { return true; }
			};
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			System.err.println("💣 Cannot install all-trusting trust manager for SSL; aborting");
			System.exit(-1);
		}
	}

	private final static Logger LOG = Logger.getLogger(DownloadServiceImpl.class.getName());

	// Download audit track
	private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
	
	// class that generates temporary URLs for S3 downloads
	AwsS3DownloadHelper awsS3DownloadHelper;
	
	public DownloadServiceImpl() throws Exception {
		
		super();
		
		awsS3DownloadHelper = new AwsS3DownloadHelper();
		
	}

	@Override
	@GET
	@Path("/download")
	@Produces({CustomMediaTypes.APPLICATION_DICOM, MediaType.APPLICATION_OCTET_STREAM})
	public Response download(
		@Context HttpServletRequest httpRequest, @Context ContainerRequestContext requestContext,
		@QueryParam("id") String id,
		@QueryParam("suppressContentDisposition") @DefaultValue("false") boolean suppressContentDisposition) {
		LOG.info("📯 HEYO! I am in the download part");

		// note: @QueryParam('id') automatically URL-decodes the 'id' value
		if (id==null) {
			return Response.status(Status.BAD_REQUEST).entity("Missing mandatory parameter 'id'").build();
		} else if (!isSafe(id)) {
			return Response.status(Status.BAD_REQUEST).entity(UNSAFE_CHARACTERS_MESSAGE).build();
		}

		try {
			String fileLocation = null;
			String realFileName = null;
			String fileName = null;
			String filePath = null;
			String name = null;
			boolean iterating_through_possibilities = false;
			
			// query Solr for file with that specific id
			SolrQuery request = new SolrQuery();
			request.setQuery("id:\""+id+"\"");
			LOG.info("🆔 HEYO! The id is «" + id + "»");
			
			// add access control
			String acfq = getAccessControlQueryStringValue(requestContext);
			LOG.info("🧏 ACFQ = " + acfq + ".");
			if (!acfq.isEmpty()) {
				request.setFilterQueries(acfq);
			}
			
			// return file location on file system or S3 + file name
			request.setFields( new String[] { SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME, SOLR_FIELD_NAME } );
			
			// note: SolrJ will URL-encode the HTTP GET parameter values
			LOG.info("❓ Executing Solr request to 'files' core: " + request.toString());
			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			LOG.info("💯 Num found: " + response.getResults().getNumFound());

			SolrDocumentList docs = response.getResults();
			Iterator<SolrDocument> iter = docs.iterator();
			while (iter.hasNext()) {
				iterating_through_possibilities = true;
				SolrDocument doc = iter.next();
				LOG.info(doc.toString());
				LOG.info("=== 1 about to get fileLocation");
				fileLocation = (String)doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
				LOG.info("=== 2 got fileLocation = «" + fileLocation + "»");
				fileName = (String)doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				realFileName = (String)doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				LOG.info("=== 3 got fileName = «" + fileName + "»");
				if (doc.getFieldValuesMap().containsKey(SOLR_FIELD_NAME)) {
					LOG.info("=== 3½ ok");
					Object nameFieldValue = doc.getFieldValue(SOLR_FIELD_NAME);
					if (nameFieldValue != null) {
						ArrayList asList = (ArrayList) nameFieldValue;
						if (asList.size() > 0) {
							String firstNameField = (String) asList.get(0);
							if (firstNameField != null && firstNameField.length() > 0) {
								LOG.info("=== 4 name field value «" + firstNameField + "» overriding fileName «" + fileName + "»");
								realFileName = firstNameField;
							}
						}
					}
				}
				filePath = fileLocation + "/" + realFileName;
				LOG.info("=== 6 filePath is «" + filePath + "»");
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
					// Download audit track
					try {
						String dn = (String) requestContext.getProperty(AuthenticationFilter.USER_DN);
						String now = iso8601.format(new Date());
						File downloadLog = new File(System.getenv("LABCAS_HOME"), "download.log");
						// TODO: rotation? Or just use Java Logging?
						PrintWriter writer = null;
						try {
							// true to FileWriter means append
							writer = new PrintWriter(new BufferedWriter(new FileWriter(downloadLog, true)));
							writer.println(now + ";" + dn + ";" + id);
						} finally {
							if (writer != null) writer.close();
						}
					} catch (IOException ex) {
						LOG.warning("Could not log this download (" + ex.getClass().getName() + ") but continuing");
						ex.printStackTrace();
						LOG.warning(ex.getMessage());
						LOG.warning("Now continuing to download the file with the download helper…");
					}

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
					"🤢 File not found or not authorized; " + iterating_through_possibilities
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


	private static String initiateZIP(String email, List<String> files) throws IOException {
		LOG.info("👀 initiateZIP for " + email + " and files " + files);

		// Newer Javas include JSON support directly; we're stuck on 1.8 so we assemble by hand:
		StringBuilder jsonPayload = new StringBuilder();
		jsonPayload.append("{");
		jsonPayload.append("\"operation\":\"initiate\",");
		jsonPayload.append("\"email\":\"").append(email).append("\",");
		jsonPayload.append("\"files\":[");
		for (int i = 0; i < files.size(); ++i) {
			jsonPayload.append("\"").append(files.get(i)).append("\"");
			if (i < files.size() - 1) jsonPayload.append(",");
		}
		jsonPayload.append("]}");
		LOG.info("👀 POST data to Zipperlab is: «" + jsonPayload + "»");
		byte[] postData = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);

		String stringURL = Parameters.getParameterValue("zipperlab");
		URL url = new URL(stringURL);
		LOG.info("👀 Zipperlab URL is " + url);

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);

		DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
		dataOutputStream.write(postData);
		dataOutputStream.flush();
		dataOutputStream.close();

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			LOG.info("👀 got OK status, so reading the UUID");
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String uuid = in.readLine();
				LOG.info("👀 Got UUID " + uuid);
				return uuid;
			} finally {
				if (in != null) in.close();
			}
		} else {
			LOG.info("🚨 Got HTTP status " + connection.getResponseCode());
			throw new IOException("Request failed with HTTP status " + connection.getResponseCode());
		}
	}

	private List<String> getFilePathsForQuery(ContainerRequestContext requestContext, String query) throws IOException {
		List<String> files = new ArrayList();
		try {
			LOG.info("🦠 Getting file paths for query " + query);

			// Start with the basic query and an empty list of files
			SolrQuery request = new SolrQuery();
			request.setQuery(query);

			// Add access control to it
			String acfq = getAccessControlQueryStringValue(requestContext);
			if (!acfq.isEmpty()) request.setFilterQueries(acfq);

			// Get the fields relevant for zipping only
			request.setFields(new String[] {
				SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME, SOLR_FIELD_NAME
			});

			// Query Solr
			LOG.info("🗄️ Query to files core «" + request + "»");
			QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
			LOG.info("℀ Number of results = " + response.getResults().getNumFound());
			Iterator<SolrDocument> iter = response.getResults().iterator();
			while (iter.hasNext()) {
				SolrDocument doc = iter.next();

				// What happens next; no idea! This is duplicated from the "download" method above
				String fileLocation = (String) doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
				String fileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				String realFileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
				if (doc.getFieldValuesMap().containsKey(SOLR_FIELD_NAME)) {
					Object nameFieldValue = doc.getFieldValue(SOLR_FIELD_NAME);
					if (nameFieldValue != null) {
						ArrayList asList = (ArrayList) nameFieldValue;
						String firstNameField = (String) asList.get(0);
						if (firstNameField != null && firstNameField.length() > 0) {
							LOG.info("🧑‍⚖️ Overriding realFileName «" + realFileName +
								 "» with firstNameField value «" + firstNameField + "»");
							realFileName = firstNameField;
						}
					}
				}
				String filePath = fileLocation + "/" + realFileName;
				LOG.info("🕵️‍♀️ Adding filePath «" + filePath + "» to list of files");
				files.add(filePath);
			}
			LOG.info("🕵️‍♀️ Returning " + files.size() + " files to query " + query);
			return files;
		} catch (SolrServerException ex) {
			LOG.warning("🔥 SolrServerException: " + ex.getMessage() + "; returning files so far (if any)");
			ex.printStackTrace();
			return files;
		}
	}

	@Override
	@GET
	@Path("/zip")
	@Produces("text/plain")
	public Response zip(
		@Context HttpServletRequest httpRequest,
		@Context ContainerRequestContext requestContext,
		@QueryParam("email") String email,
		@QueryParam("query") String query
	) {
		LOG.info("👀 I see you, " + email + ", with your zip request for " + query);
		try {
			LOG.info("👀 getting uuid");
			String uuid = initiateZIP(email, getFilePathsForQuery(requestContext, query));
			LOG.info("👀 uuid is " + uuid);
			return Response.status(Status.OK).entity(uuid).build();
		} catch (IOException ex) {
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
