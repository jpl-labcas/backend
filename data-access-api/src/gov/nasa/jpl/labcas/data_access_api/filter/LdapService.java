package gov.nasa.jpl.labcas.data_access_api.filter;

import java.util.List;

/**
 * Specification of service used to query the 
 * LDAP (Lightweight Directory Access Protocol) database.
 * 
 * @author Luca Cinquini
 *
 */
public interface LdapService {

	/**
	 * Method to authenticate the user credentials.
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	boolean authenticate(String username, String password);
	
	/**
	 * Method to retrieve the groups a user belongs to.
	 * @param userdn
	 * @return
	 */
	List<String> authorize(String userdn);

}