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
	 * Method to authenticate user credentials.
	 * 
	 * @param authCredentials : single string of encoded credentials as supplied in the HTTP Basic Authentication header.
	 * @return
	 */
	boolean authenticate(String authCredentials);

}