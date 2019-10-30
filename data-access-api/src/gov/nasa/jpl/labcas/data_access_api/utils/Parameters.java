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
	
	private static Properties properties = new Properties();
	
	static {
		try {
			InputStream input = new FileInputStream(System.getProperty("user.home") + LABCAS_PROPERTIES);
			properties.load(input);
			Enumeration<Object> keys = properties.keys();
			while (keys.hasMoreElements()) {
			    String key = (String)keys.nextElement();
			    String value = (String)properties.get(key);
			    if (key.toLowerCase().contains("password")) {
			    	LOG.info("Using parameter key=" + key + " value=....");
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
