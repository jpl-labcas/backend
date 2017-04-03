package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.oodt.cas.crawl.action.CrawlerAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

/**
 * Class that publishes images of compatible type to the QUIP Image Viewer.
 * 
 * @author cinquini
 *
 */
public class QuipImageViewerPostIngestionAction extends CrawlerAction {
	
    // QUIP publishing endpoint
    private String quipImageViewerUrl = "http://localhost:6002/submitData";
    
	// compatible file extensions
    private String extensions = "";
    
    private Set<String> extensionsSet = new HashSet<String>();

	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
		
		// determine file extension
		String extension = GeneralUtils.getFileExtension(product).toLowerCase();
		
		// process compatible extensions
		if (this.extensionsSet.contains(extension)) {
			this.uploadFile(product);
		}
		
		// success
		return true;
		
	}
	
	/**
	 * Method to upload a file via a multi-part/form-data POST request to the QUIP server.
	 */
	private void uploadFile(File product) {
		
		LOG.info("QUIP: uploading file: "+product.getAbsolutePath());

		HttpClient httpclient = new DefaultHttpClient();
		HttpEntity resEntity = null;
		
		try {
			
			HttpPost httppost = new HttpPost(this.quipImageViewerUrl);

			FileBody upload = new FileBody(product);
			StringBody case_id = new StringBody(product.getName());

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("upload", upload);
			reqEntity.addPart("case_id", case_id);
			httppost.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(httppost);
			resEntity = response.getEntity();
			LOG.info("QUIP upload result="+resEntity.toString());

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
	 */
	@Override
	public void validate() throws CrawlerActionException {
				
		String[] extensionsArray = extensions.split(",");
		for (String ext : extensionsArray) {
			extensionsSet.add(ext.toLowerCase());
		}
		LOG.info("QUIP will process these file extensions: "+extensionsSet);		
		
	}
	
    public void setQuipImageViewerUrl(String quipImageViewerUrl) {
		this.quipImageViewerUrl = quipImageViewerUrl;
	}

	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}


}
