package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;
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
		String extension = "";
		String fileName = product.getName();
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1).toLowerCase();
		}
		
		if (this.extensionsSet.contains(extension)) {
			LOG.info("Performing QUIP publishing for file: "+product.getAbsolutePath());
			
			if (fileName.equals("EX10-0061-3N-PR.svs")) {
				this.uploadFile(product);
			}
			
		} else {
			LOG.info("QUIP: extension NOT found="+extension);
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
		
		try {
						
			// OLD API
			
			
			HttpPost httppost = new HttpPost("http://localhost:6002/submitData");

			String fileName = "/usr/local/labcas_staging/Team_37_CTIIP_Animal_Models/CTIIP-1.1b._Mouse/1/EX10-0061-3N-ER.svs";
			FileBody bin = new FileBody(new File(fileName));
			StringBody comment = new StringBody("EX10-0061-3N-ER-NEW");

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("upload", bin);
			reqEntity.addPart("case_id", comment);
			httppost.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();
			//LOG.info("QUIP upload result="+resEntity.toString());

		} catch(Exception e) {
			LOG.warning("QUIP upload resulted in error: "+e.getMessage());
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
