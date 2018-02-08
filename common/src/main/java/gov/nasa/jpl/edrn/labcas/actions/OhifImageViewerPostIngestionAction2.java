package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.oodt.cas.crawl.action.ExternAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

public class OhifImageViewerPostIngestionAction2 extends ExternAction {
	
	// compatible file extensions
    private String extensions = "";
    
    private Set<String> extensionsSet = new HashSet<String>();
    
	/**
	 * File ~/labcas.properties
	 */
    private Properties properties = new Properties();

	
	public OhifImageViewerPostIngestionAction2() {
		super();
	}
	
	/**
	 * Executes the external command if the file matches one of the designated extensions.
	 * 
	 * @param product
	 * @param productMetadata
	 * @return
	 * @throws CrawlerActionException
	 */
	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
				
		// only proceed if NOOHIF flag is not set
		if ( !productMetadata.containsKey(Constants.METADATA_KEY_NOOHIF) ) {
		
			// determine file extension
			String extension = GeneralUtils.getFileExtension(product).toLowerCase();
			
			// process compatible extensions
			if (this.extensionsSet.contains(extension)) {
				return super.performAction(product, productMetadata);
			}
			
		}
		
		// success if no action is taken
		return true;
		
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
		
		// FIXME
		// read OHIF configuration from labcas.properties
		/**
        this.ohifSubmitImageUrl = properties.getProperty("ohifSubmitImageUrl");
        LOG.info("Using ohifSubmitImageUrl="+ohifSubmitImageUrl);
        this.ohifViewImageUrl = properties.getProperty("ohifViewImageUrl");
        LOG.info("Using ohifViewImageUrl="+ohifViewImageUrl);
        this.ohifUsername = properties.getProperty("ohifUsername");
        this.ohifPassword = properties.getProperty("ohifPassword");
        */
		
	}

	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}
		
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
}
