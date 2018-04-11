package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlUtils {
	
	/**
	 * Properly URL-encodes a value, using '%20' for spaces, not '+'
	 * @param value
	 * @return
	 */
	public static String encode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
	}

}
