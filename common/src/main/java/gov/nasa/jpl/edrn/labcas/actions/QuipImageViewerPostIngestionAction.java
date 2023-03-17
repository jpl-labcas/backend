package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.oodt.cas.crawl.action.CrawlerAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;


/**
 * Class that publishes images of compatible type to the QUIP Image Viewer.
 * 
 * @author Luca Cinquini
 *
 */
public class QuipImageViewerPostIngestionAction extends CrawlerAction {
	
    // QUIP publishing endpoint
    private String quipSubmitImageUrl = "http://localhost:6002/submitData";
    
    // QUIP display endpoint
    private String quipViewImageUrl = "http://localhost:8000/camicroscope/osdCamicroscope.php?tissueId=";
    
	// compatible file extensions
    private String extensions = "";
    
    private Set<String> extensionsSet = new HashSet<String>();
    
	/**
	 * File ~/labcas.properties
	 */
    private Properties properties = new Properties();

	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
				
		// only proceed if NOQUIP flag is not set
		if ( !productMetadata.containsKey(Constants.METADATA_KEY_NOQUIP) ) {
		
			// determine file extension
			String extension = GeneralUtils.getFileExtension(product).toLowerCase();
			
			// process compatible extensions
			if (this.extensionsSet.contains(extension)) {
				
				// do NOT upload file
				//this.uploadFile(product, productMetadata);
				
				// but still publish the QUIP URL
				String fileId = product.getName().replaceAll("."+extension, "");
				String caMicroscopeUrl =  this.quipViewImageUrl + "?tissueId=" + fileId;
				productMetadata.addMetadata("FileUrl", caMicroscopeUrl);
				productMetadata.addMetadata("FileUrlType", Constants.URL_TYPE_CAMICROSCOPE );
				LOG.info("QUIP: set caMicroscope URL: "+ caMicroscopeUrl);
				
			}
			
		}
		
		// success
		return true;
		
	}
	
	/**
	 * Method to upload a file via a multi-part/form-data POST request to the QUIP server.
	 */
	private void uploadFile(File product, Metadata productMetadata) {
		
		LOG.info("QUIP: uploading file: "+product.getAbsolutePath());

		HttpClient httpclient = null;
		try {
			httpclient = HttpClients.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			System.err.println("I give up in QuipImageViewerPostIngestionAction.uploadFile");
			System.exit(42);
		}

		HttpEntity resEntity = null;
		
		try {
			
			HttpPost httppost = new HttpPost(this.quipSubmitImageUrl);

			// NOTE: must rename the file on upload because non-standard characters
			// (such as ' or ~) are not suitable for QUIP image identifiers
			String filename = UUID.randomUUID().toString() + ".svs";
			FileBody upload = new FileBody(product, filename, "application/octet-stream", "UTF-8");
			            
			StringBody case_id = new StringBody(filename);

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("upload", upload);
			reqEntity.addPart("case_id", case_id);
			httppost.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(httppost);
			resEntity = response.getEntity();
			LOG.info("QUIP upload result="+resEntity.toString());
			
			// add URL to metadata
			productMetadata.addMetadata("FileUrl", this.quipViewImageUrl + "?tissueId=" + filename);
			productMetadata.addMetadata("FileUrlType", Constants.URL_TYPE_CAMICROSCOPE );

		} catch(Exception e) {
			LOG.warning("QUIP upload resulted in error: "+e.getMessage());
			
		} finally {
			try {
				if (resEntity != null) {
					InputStream instream = resEntity.getContent();
					instream.close();
				}
			} catch(Exception e) {}
		}

    }
	
	/**
	 * Converts the 'extensions' String into a Set.
	 * Also retrieves the QUIP server end-points from the properties values.
	 */
	@Override
	public void validate() throws CrawlerActionException {
			
		
		String[] extensionsArray = extensions.split(",");
		for (String ext : extensionsArray) {
			extensionsSet.add(ext.toLowerCase());
		}
		LOG.info("QUIP will process these file extensions: "+extensionsSet);		
		
		// read QUIP URLs from labcas.properties
        this.quipSubmitImageUrl = properties.getProperty("quipSubmitImageUrl");
        LOG.info("Using quipSubmitImageUrl="+quipSubmitImageUrl);
        this.quipViewImageUrl = properties.getProperty("quipViewImageUrl");
        LOG.info("Using quipViewImageUrl="+quipViewImageUrl);
		
	}
	
	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}
		
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
