package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.binary.Base64;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

/**
 * Service implementation used to query the LDAP database.
 * 
 * @author Luca Cinquini
 *
 */
public class LdapServiceImpl implements LdapService {

	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());

	// read from ~/labcas.properties
	private final static String LDAP_USERS_URI_PROPERTY = "ldapUsersUri";
	private final String ldapUsersUri;
	private final static String LDAP_GROUPS_URI_PROPERTY = "ldapGroupsUri";
	private final String ldapGroupsUri;
	private final static String LDAP_DN_PATTERN_PROPERTY = "ldapDnPattern";
	private final String ldapDnPattern;
	private final static String LDAP_ADMIN_DN_PROPERTY = "ldapAdminDn";
	private final String ldapAdminDn;
	private final static String LDAP_ADMIN_PASSWORD = "ldapAdminPassword";
	private final String ldapAdminPassword;
	
	private final static String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

	public LdapServiceImpl() {
		
		ldapUsersUri = Parameters.getParameterValue(LDAP_USERS_URI_PROPERTY);
		ldapGroupsUri = Parameters.getParameterValue(LDAP_GROUPS_URI_PROPERTY);
		ldapDnPattern = Parameters.getParameterValue(LDAP_DN_PATTERN_PROPERTY);
		ldapAdminDn = Parameters.getParameterValue(LDAP_ADMIN_DN_PROPERTY);
		ldapAdminPassword = Parameters.getParameterValue(LDAP_ADMIN_PASSWORD);
		
	}

	@Override
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
		String dn = buildDn(username);
		//LOG.info("Testing LDAP binding for: " + dn);
		LOG.info("Testing LDAP binding for: " + dn + " password="+password);
		return bind(dn, password);

	}
	
	public String buildDn(String username) {
		return this.ldapDnPattern.replaceAll("@USERNAME@", username);
	}

	/**
	 * Authenticates (dn, password) credentials versus the LDAP user database.
	 * 
	 * @param dn
	 * @param password
	 * @return
	 * @throws Exception
	 */
	protected boolean bind(String dn, String password) {

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
	

	/**
	 * Queries LDAP for given username.
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private String getUid(String username) throws Exception {
		
		DirContext ctx = ldapContext();

		String filter = "(uid=" + username + ")";
		SearchControls ctrl = new SearchControls();
		ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
		NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctrl);

		String dn;
		if (answer.hasMore()) {
			SearchResult result = (SearchResult) answer.next();
			dn = result.getNameInNamespace();
		}
		else {
			dn = null;
		}
		answer.close();
		return dn;
		
	}
	
	/**
	 * Creates an LDAP context using the administrator credentials.
	 * 
	 * @return
	 * @throws Exception
	 */
	private DirContext ldapContext () throws Exception {
		
		Hashtable<String,String> env = new Hashtable <String,String>();
		
		// use LDAP admin credentials
		env.put(Context.SECURITY_PRINCIPAL, ldapAdminDn);
		env.put(Context.SECURITY_CREDENTIALS, ldapAdminPassword);
	
		return ldapContext(env);
	}

	private DirContext ldapContext (Hashtable <String,String>env) throws Exception {
		env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
		env.put(Context.PROVIDER_URL, ldapUsersUri);
		DirContext ctx = new InitialDirContext(env);
		return ctx;
	}
	
	/**
	 * Method to debug LDAP connection
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		String user = args[0];
		String password = args[1];
		
		// use admin credentials to find user
		LdapServiceImpl self = new LdapServiceImpl();
		String dn = self.getUid( user );
		
		if (dn != null) {
			
			// found user - test password
			if ( self.bind( dn, password ) ) {
				System.out.println( "User '" + user + "' authentication succeeded" );
			} else {
				System.out.println( "User '" + user + "' authentication failed" );
			}
			
		} else {
			System.out.println( "User '" + user + "' not found" );
		}
		
		//LOG.info("Testing LDAP binding for: " + dn + " password="+password);
		//boolean status = self.bind(dn, password);
		//LOG.info("Authentication status="+status);
		
	}

}