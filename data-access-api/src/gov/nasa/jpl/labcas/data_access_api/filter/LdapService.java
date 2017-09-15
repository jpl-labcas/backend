package gov.nasa.jpl.labcas.data_access_api.filter;

/**
 * Specification of service used to query the 
 * LDAP (Lightweight Directory Access Protocol) database.
 * 
 * @author Luca Cinquini
 *
 */
public interface LdapService {

	/**
	 * Method to authenticate user credentials and retrieve the user groups.
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	boolean authenticate(String username, String password);

}