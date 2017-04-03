package gov.nasa.jpl.edrn.labcas.utils;

import java.io.File;

/**
 * Class containing generic utility methods.
 * @author cinquini
 *
 */
public class GeneralUtils {
	
	/**
	 * Utility method to remove non-ASCII characters from a string.
	 * @param string
	 * @return
	 */
	public static String removeNonAsciiCharacters(String string) {
		return string.replaceAll("[^\\x00-\\x7F]", "");
	}
	
	/**
	 * Utility method to extract the file extension.
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileExtension(File file) {
		
		String extension = "";
		String fileName = file.getName();
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}
		
		return extension;

	}

}
