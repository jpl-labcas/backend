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
 * Class responsible for writing product/file level metadata:
 * o reads all metadata from XML file <filename>.xmlmet in the same directory
 * o transfers product-level metadata fields: DatasetId, Version
 * o disregards all other product-level metadata
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
		
		// transfer selected product-level metadata
		//outmet.addMetadata(inmet.getHashtable()); // transfer ALL product level metadata
		outmet.addMetadata(Constants.METADATA_KEY_DATASET_ID, inmet.getMetadata(Constants.METADATA_KEY_DATASET_ID));
		outmet.addMetadata(Constants.METADATA_KEY_VERSION, inmet.getMetadata(Constants.METADATA_KEY_VERSION));
		
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
				outmet.addMetadata(fmet.getHashtable());
			}
		}
		
		return outmet;
	}

}
