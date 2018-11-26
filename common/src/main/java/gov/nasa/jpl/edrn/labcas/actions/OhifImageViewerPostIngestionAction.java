package gov.nasa.jpl.edrn.labcas.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.oodt.cas.crawl.action.CrawlerAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;
import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

/**
 * Class that publishes DICOM images to the Open Health Image Foundation (OHIF) Viewer.
 * 
 * @author Luca Cinquini
 *
 */
public class OhifImageViewerPostIngestionAction extends CrawlerAction {
	
    // OHIF publishing endpoint
    private String ohifSubmitImageUrl = "http://localhost:8042/instances";
    
    // OHIF display endpoint
    private String ohifViewImageUrl = "http://localhost:3000/viewer/";
    
    // OHIF credentials
    private String ohifUsername = null;
    private String ohifPassword = null;
    
	// compatible file extensions
    private String extensions = "";
    
    private Set<String> extensionsSet = new HashSet<String>();
    
	/**
	 * File ~/labcas.properties
	 */
    private Properties properties = new Properties();

	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
				
		// only proceed if NOOHIF flag is not set
		if ( !productMetadata.containsKey(Constants.METADATA_KEY_NOOHIF) ) {
		
			// determine file extension
			String extension = GeneralUtils.getFileExtension(product).toLowerCase();
			
			// process compatible extensions
			if (this.extensionsSet.contains(extension)) {
				this.uploadFile(product, productMetadata);
			}
			
		}
		
		// success
		return true;
		
	}
	
	/**
	 * Method to upload a file via a multi-part/form-data POST request to the OHIF server.
	 */
	private void uploadFile(File product, Metadata productMetadata) {
		
		String studyInstanceId = productMetadata.getMetadata("_File_labcas.dicom:StudyInstanceUID");
		LOG.info("OHIF: uploading file: "+product.getAbsolutePath()+" StudyInstanceUID="+studyInstanceId);

		FileManagerUtils.printMetadata(productMetadata);
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpEntity resEntity = null;
		
		try {
			
			HttpPost httppost = new HttpPost(this.ohifSubmitImageUrl);

			FileBody upload = new FileBody(product, product.getName(), "application/octet-stream", "UTF-8");
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("upload", upload);
			httppost.setEntity(reqEntity);
			
			httppost.addHeader("Expect:", null);
			
			byte[] encodedBytes = Base64.encodeBase64((this.ohifUsername+":"+this.ohifPassword).getBytes());
			httppost.setHeader("Authorization", "Basic " + new String(encodedBytes));

			HttpResponse response = httpclient.execute(httppost);
			resEntity = response.getEntity();
			LOG.info("Upload result="+resEntity.toString());
			
			// add URL to metadata
			productMetadata.addMetadata("FileUrl", this.ohifViewImageUrl + studyInstanceId);
			productMetadata.addMetadata("FileUrlType", Constants.URL_TYPE_OHIF);

			
		} catch(Exception e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (resEntity != null) {
					InputStream instream = resEntity.getContent();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
		            String line = null;
		            while ((line = reader.readLine()) != null) {
		                LOG.info(line);
		            }
		            reader.close();
					instream.close();
				}
			} catch(Exception e) {}
		}

    }
	
	/**
	 * Converts the 'extensions' String into a Set.
	 * Also retrieves the OHIF server end-points from the properties values.
	 */
	@Override
	public void validate() throws CrawlerActionException {
			
		
		String[] extensionsArray = extensions.split(",");
		for (String ext : extensionsArray) {
			extensionsSet.add(ext.toLowerCase());
		}
		LOG.info("OHIF will process these file extensions: "+extensionsSet);		
		
		// read OHIF configuration from labcas.properties
        this.ohifSubmitImageUrl = properties.getProperty("ohifSubmitImageUrl");
        LOG.info("Using ohifSubmitImageUrl="+ohifSubmitImageUrl);
        this.ohifViewImageUrl = properties.getProperty("ohifViewImageUrl");
        LOG.info("Using ohifViewImageUrl="+ohifViewImageUrl);
        this.ohifUsername = properties.getProperty("ohifUsername");
        this.ohifPassword = properties.getProperty("ohifPassword");
		
	}
	
	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}
		
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
