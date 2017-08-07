package gov.nasa.jpl.edrn.labcas.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.CasMetadataException;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;
import org.apache.oodt.commons.exceptions.CommonsException;

/**
 * Class that generates a thumbnail for images that can be read with the OpenSlide library.
 * This class is a wrapper to a python script that uses the Python OpenSlide bindings ("openslide-python")..
 * 
 * @author luca
 *
 */
public class OpenSlideThumbnailGenerator extends PcsMetFileWriter  {
	
	private static final Logger LOG = Logger.getLogger(OpenSlideThumbnailGenerator.class.getName());
	
	/**
	 * Configuration file located in user home directory
	 */
	private final static String LABCAS_PROPERTIES = "/labcas.properties";
	
	/**
	 * File ~/labcas.properties
	 */
	private Properties properties = new Properties();
	
    private String thumbnailsRootDir = "/tmp";
    
    private String thumbnailsRootUrl = "http://localhost/";
    
    /** Constructor reads in the thumbnails location, URLs from the configuration properties. */
    public OpenSlideThumbnailGenerator() {
    	
    	LOG.info("Initializing OpenSlideThumbnailGenerator");
    	
    	try {
			InputStream input = new FileInputStream(System.getProperty("user.home") + LABCAS_PROPERTIES);
			properties.load(input);
            this.thumbnailsRootDir = properties.getProperty("thumbnailsRootDir");
            this.thumbnailsRootUrl = properties.getProperty("thumbnailsRootUrl");
            LOG.info("Using thumbnailsRootDir="+this.thumbnailsRootDir+", thumbnailsRootUrl="+this.thumbnailsRootUrl );

    	} catch(IOException e) {
    		LOG.warning("Eroor reading property file: " + LABCAS_PROPERTIES);
    	}
    	
    }

		
	@Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile, 
    		                                     Metadata inputMetadata, 
    		                                     Object... customArgs)
        throws FileNotFoundException, ParseException, CommonsException, CasMetadataException {
    	
    	Metadata metadata = new Metadata();
    	
    	LOG.info("Executing OpenSlideThumbnailGenerator for file: "+sciPgeCreatedDataFile.getAbsolutePath());
    	
        for (Object arg : customArgs) {
        	
	    	// system command to execute
	    	String command = (String)arg + " " +sciPgeCreatedDataFile.getAbsolutePath() + " " + this.thumbnailsRootDir + " " + this.thumbnailsRootUrl;
	    	LOG.info("Executing command: "+command);
	        
	    	try {
	        	
	        	final Runtime r = Runtime.getRuntime();
	            final Process p = r.exec(command);
	            final int returnCode = p.waitFor();
	
	            final BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            
	            String line;
	            while ((line = is.readLine()) != null) {
	                LOG.info(line);
	                // parse standard output from script into metadata
	                line = line.replaceAll("\n", "");
	                String[] parts = line.split("=");
	                metadata.addMetadata(parts[0].trim(), parts[1].trim());
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
        
        return metadata;
        
    }
    

}
