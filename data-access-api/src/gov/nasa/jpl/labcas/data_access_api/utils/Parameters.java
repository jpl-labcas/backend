package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class containing application-level parameters and constants.
 * Parameter values are read at startup from the property file ~/labcas.properties.
 */
public class Parameters {
	
	private final static Logger LOG = Logger.getLogger(Parameters.class.getName());
	
	/**
	 * Configuration file located in user home directory
	 */
	private final static String LABCAS_PROPERTIES = "/labcas.properties";

	// Default Zipperlab URL
	private static final String DEFAULT_ZIPPERLAB_URL = "https://edrn-docker/ops/zipperlab";  // Default Zipperlab URL
	
	// Default max number of rows we allow Solr to return; note this must be a string because of the way
	// this class was originally coded.
	private static final String DEFAULT_MAX_ROWS = "5000";

	private static Properties properties = new Properties();
	
	static {
		try {
			InputStream input = new FileInputStream(System.getProperty("user.home") + LABCAS_PROPERTIES);

			// Default Zipperlab URL
			properties.put("zipperlab", DEFAULT_ZIPPERLAB_URL);  // Default Zipperlab URL

			// Default max number of rows
			properties.put("max_solr_rows", DEFAULT_MAX_ROWS);  // Default max number of rows

			// Override defaults (which right now is default Zipperlab URL) with those in ~/labcas.properties:
			properties.load(input);

			// Show your work
			Enumeration<Object> keys = properties.keys();
			while (keys.hasMoreElements()) {
			    String key = (String)keys.nextElement();
			    String value = (String)properties.get(key);
			    if (key.toLowerCase().contains("password")) {
			    	LOG.info("Using parameter key=" + key + " value…");
			    } else {
			    	LOG.info("Using parameter key=" + key + " value=" + value);
			    }
			}

		} catch (IOException e) {
			LOG.warning("Eroor reading property file: " + LABCAS_PROPERTIES);
		}
	}
	
	public static String getParameterValue(String parameterName) {
		return properties.getProperty( parameterName );
	}

}
