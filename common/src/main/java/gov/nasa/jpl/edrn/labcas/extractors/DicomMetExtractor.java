package gov.nasa.jpl.edrn.labcas.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.CasMetadataException;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;
import org.apache.oodt.commons.exceptions.CommonsException;

import gov.nasa.jpl.edrn.labcas.utils.FileManagerUtils;

/**
 * Class responsible extracting metadata from a DICOM file.
 * This class is a wrapper to a python script using the pydicom module.
 * 
 * @author luca
 *
 */
public class DicomMetExtractor extends PcsMetFileWriter  {
	
	private static final Logger LOG = Logger.getLogger(DicomMetExtractor.class.getName());
		
    @Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile, 
    		                                     Metadata inputMetadata, 
    		                                     Object... customArgs)
        throws FileNotFoundException, ParseException, CommonsException, CasMetadataException {
                
    	// loop over extraction commands, passing in the DICOM file location
        for (Object arg : customArgs) {
        
        	try {

        		String command = (String)arg + " " +sciPgeCreatedDataFile.getAbsolutePath();
	        	LOG.info("Executing command: "+command);
	        	final Runtime r = Runtime.getRuntime();
	            final Process p = r.exec(command);
	            final int returnCode = p.waitFor();
	
	            final BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            String line;
	            while ((line = is.readLine()) != null) {
	                LOG.warning(line);
	            }
	            final BufferedReader is2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	            while ((line = is2.readLine()) != null) {
	            	LOG.warning(line);
	            }
	            LOG.info("Command returned with value: "+returnCode);
	            
        	} catch(IOException ioe) {
        		LOG.warning(ioe.getMessage());
        	} catch(InterruptedException ie) {
	    		LOG.warning(ie.getMessage());
	    	}

        }

        // read back the metadata written by the script
        File xmlmetFile = new File(sciPgeCreatedDataFile.getAbsolutePath()+".xmlmet");
        LOG.info("Reading metadata file: "+xmlmetFile.getAbsolutePath());
        Metadata outmet = FileManagerUtils.readMetadataFromFile(xmlmetFile);
        return outmet;
        
    }

}
