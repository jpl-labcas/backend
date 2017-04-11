package gov.nasa.jpl.edrn.labcas.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility class to detect the mime type of a file.
 * 
 * @author cinquini
 *
 */
public class MimeTypeUtils {
	
	// mime-types.properties is located at the root of the class path
	public static String MIME_TYPE_FILENAME = "mime-types.properties";
	
	private static final Logger LOG = Logger.getLogger(FileManagerUtils.class.getName());
	
	private static Map<String, String> mimeTypeMap = new HashMap<String, String>();
	
	/**
	 * static initializer loads the mime type property file from the classpath
	 */
	static {
		
		InputStream input = null;
		Properties props = new Properties();
		
		try {
	
			// specify file location relative to classpath root
			LOG.info("Loading property file="+MIME_TYPE_FILENAME);
			input = MimeTypeUtils.class.getClassLoader().getResourceAsStream(MIME_TYPE_FILENAME);
			props.load(input);
			LOG.info("Loaded properties="+props);
			
			for (String key : props.stringPropertyNames()) {
				String value = props.getProperty(key);
				LOG.info("Matching extension: "+key+" to mime type: " + value);
				mimeTypeMap.put(key.toLowerCase(), value.toLowerCase());
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
			LOG.warning(ex.getMessage());
			
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Method that looks up the file extension in the static map of mime types.
	 * @param filePath
	 * @return
	 */
	public static String getMimeType(String filePath) {
		
		LOG.info("Detecting FileType for product: "+filePath);
		String ext = FilenameUtils.getExtension(filePath).toLowerCase();
		
		if (mimeTypeMap.containsKey(ext)) {
			return mimeTypeMap.get(ext);
		} else {
			return null;
		}
		
	}
	
}
