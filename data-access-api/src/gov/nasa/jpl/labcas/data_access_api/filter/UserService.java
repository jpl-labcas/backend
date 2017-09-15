package gov.nasa.jpl.labcas.data_access_api.filter;

import java.util.List;

/**
 * Specification of service used to query the 
 * LDAP (Lightweight Directory Access Protocol) database.
 * 
 * @author Luca Cinquini
 *
 */
public interface UserService {

	/**
	 * Method to validate the user credentials.
	 * 
	 * @param username
	 * @param password
	 * @return user DN if credentials are valid, null otherwise
	 */
	String getValidUser(String username, String password);
	
	/**
	 * Method to retrieve the groups a user belongs to.
	 * @param userdn
	 * @return
	 */
	List<String> getUserGroups(String userdn);

}