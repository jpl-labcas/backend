package gov.nasa.jpl.edrn.labcas.extractors;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.metadata.extractors.FilemgrMetExtractor;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.MetExtractionException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.FileManagerUtils;

/**
 * Class that reads product-level metadata from an XML file 
 * with the same name in the same directory.
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
		outmet.addMetadata(inmet.getHashtable());
		
		// construct path to optional metadata file
		File xmlmetFilepath = new File(inmet.getMetadata(Constants.METADATA_KEY_FILE_LOCATION), 
									   inmet.getMetadata(Constants.METADATA_KEY_FILE_NAME)+Constants.EDRN_METADATA_EXTENSION);

		// add metadata from file
		if (xmlmetFilepath.exists()) {
			LOG.info("Adding additional product metadata from file: "+xmlmetFilepath.getAbsolutePath());
			
			Metadata fmet = FileManagerUtils.readMetadata(xmlmetFilepath);
			outmet.addMetadata(fmet.getHashtable());
			
		}
		
		return outmet;
	}

}
