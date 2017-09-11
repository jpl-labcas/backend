package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.codec.binary.Base64;

/**
 * Service used to query the LDAP database.
 * 
 * @author Luca Cinquini
 *
 */
public class AuthenticationService {

	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());

	// FIXME: read from ~/labcas.properties
	private final static String ldapUsersUri = "ldaps://edrn.jpl.nasa.gov:636/ou=users,o=MCL";
	private final static String ldapGroupsUri = "ldaps://edrn.jpl.nasa.gov:636/ou=groups,o=MCL";
	
	private final static String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

	public AuthenticationService() {}

	public boolean authenticate(String authCredentials) {

		if (null == authCredentials) {
			return false;
		}

		// extract username and password from encoded HTTP 'Authorization' header
		// header value format will be "Basic encodedstring" for Basic authentication.
		// Example "Basic YWRtaW46YWRtaW4="
		final String encodedUserPassword = authCredentials.replaceFirst("Basic" + " ", "");
		String usernameAndPassword = null;
		try {
			byte[] decodedBytes = Base64.decodeBase64(encodedUserPassword.getBytes());
			usernameAndPassword = new String(decodedBytes, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
		final String username = tokenizer.nextToken();
		final String password = tokenizer.nextToken();

		// authenticate user by 'binding' to the LDAP server
		String dn = "uid=" + username + ",ou=users,o=MCL"; // FIXME
		LOG.info("Testing LDAP binding for DN=" + dn + " password=...");
		return bind(dn, password);

	}

	/**
	 * Authenticates (dn, password) credentials versus the LDAP user database.
	 * 
	 * @param dn
	 * @param password
	 * @return
	 * @throws Exception
	 */
	private boolean bind(String dn, String password) {

		try {
			
			// set up the environment for creating the initial context
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
			env.put(Context.PROVIDER_URL, ldapUsersUri);
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			
			// authentication parameters
			env.put(Context.SECURITY_PRINCIPAL, dn);
			env.put(Context.SECURITY_CREDENTIALS, password);

			// create the initial context
			DirContext ctx = new InitialDirContext(env);
			boolean result = ctx != null;

			if (ctx != null)
				ctx.close();

			return result;
			
		} catch (Exception e) {
			return false;
		}

	}

}