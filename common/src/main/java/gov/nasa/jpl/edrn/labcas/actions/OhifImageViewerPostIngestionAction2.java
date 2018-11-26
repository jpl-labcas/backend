package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;

import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Crawler action that publishes a DICOM image to the DCM4CHEE backend
 * by invoking a client tool inside a Docker container.
 * 
 * @author cinquini
 *
 */
public class OhifImageViewerPostIngestionAction2 extends SshExternAction {
	
    // OHIF display endpoint
    private String ohifViewImageUrl = "http://localhost:3000/viewer/";
	
	public OhifImageViewerPostIngestionAction2() {
		super();
	}
	
	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
		
		// only proceed if NOOHIF flag is not set
		if (!productMetadata.containsKey(Constants.METADATA_KEY_NOOHIF)) {
			
			String studyInstanceId = productMetadata.getMetadata("_File_labcas.dicom:StudyInstanceUID");
			LOG.info("OHIF: uploading file: "+product.getAbsolutePath()+" StudyInstanceUID="+studyInstanceId);

			boolean status = super.performAction(product, productMetadata);
			
			if (status) {
				
				// add OHIF viewer URL to the product metadata
				productMetadata.addMetadata("FileUrl", this.ohifViewImageUrl + studyInstanceId);
				productMetadata.addMetadata("FileUrlType", Constants.URL_TYPE_OHIF);

			}
			
		} 
		
		// success if no action is taken
		return true;
		
	}
	
	@Override
	public void validate() throws CrawlerActionException {

		super.validate();
		
        this.ohifViewImageUrl = properties.getProperty("ohifViewImageUrl");
        LOG.info("Using ohifViewImageUrl="+ohifViewImageUrl);

	}

}
