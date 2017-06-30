package gov.nasa.jpl.edrn.labcas.extractors;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.metadata.extractors.FilemgrMetExtractor;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.MetExtractionException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * Class responsible for reading additional metadata from the .met and .xmlmet files on the server.
 * IMPORTANT: this class is currently NOT used.
 * 
 * @author luca
 *
 */
public class XmlFileMetExtractor implements FilemgrMetExtractor {
	
	private static final Logger LOG = Logger.getLogger(XmlFileMetExtractor.class.getName());

	@Override
	public void configure(Properties properties) {}

	@Override
	public Metadata extractMetadata(Product product, Metadata inmet) throws MetExtractionException {
		
		LOG.info("Running XmlFileMetExtractor for product:" + product.getProductName());
		
		// merge original metadata
		Metadata outmet = new Metadata();
				
		// construct paths to optional metadata files
		// read both <filename>.met and <filename>.xmlmet
		File[] xmlmetFilepaths = new File[]{ new File(inmet.getMetadata(Constants.METADATA_KEY_FILE_LOCATION), 
									                  inmet.getMetadata(Constants.METADATA_KEY_FILE_NAME)+Constants.OODT_METADATA_EXTENSION),
		                                     new File(inmet.getMetadata(Constants.METADATA_KEY_FILE_LOCATION), 
										              inmet.getMetadata(Constants.METADATA_KEY_FILE_NAME)+Constants.EDRN_METADATA_EXTENSION)};

		// add metadata from file
		for (File xmlmetFilepath :xmlmetFilepaths) {
			if (xmlmetFilepath.exists()) {
				LOG.info("Adding additional product metadata from file: "+xmlmetFilepath.getAbsolutePath());
				Metadata fmet = FileManagerUtils.readMetadataFromFile(xmlmetFilepath);
				outmet.addMetadata(fmet.getMap());
			}
		}
		
		return outmet;
	}

}
