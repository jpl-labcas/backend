package gov.nasa.jpl.edrn.labcas.utils;

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

}
