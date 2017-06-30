package gov.nasa.jpl.edrn.labcas.extractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.CasMetadataException;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;
import org.apache.oodt.commons.exceptions.CommonsException;

import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * Class that reads file-level metadata 
 * from the .xmlmet file co-located with the product file.
 * 
 * @author luca
 *
 */
public class XmlFileMetadataReader extends PcsMetFileWriter  {
	
	private static final Logger LOG = Logger.getLogger(XmlFileMetadataReader.class.getName());
		
    @Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile, 
    		                                     Metadata inputMetadata, 
    		                                     Object... customArgs)
        throws FileNotFoundException, ParseException, CommonsException, CasMetadataException {
    	
    	LOG.info("Executing XmlFileMetadataReader");
                
        File xmlmetFile = new File(sciPgeCreatedDataFile.getAbsolutePath()+".xmlmet");
        
        if (xmlmetFile.exists()) {
        	LOG.info("Reading metadata file: "+xmlmetFile.getAbsolutePath());
        	Metadata outmet = FileManagerUtils.readMetadataFromFile(xmlmetFile);
        	return outmet;
        
        } else {
        	LOG.info("Metadata file: "+xmlmetFile.getAbsolutePath()+" not found");
        	return new Metadata(); // empty metadata
        }
        
    }

}