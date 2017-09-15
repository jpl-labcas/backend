package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
	private final static String LDAP_ADMIN_DN_PROPERTY = "ldapAdminDn";
	private final String ldapAdminDn;
	private final static String LDAP_ADMIN_PASSWORD = "ldapAdminPassword";
	private final String ldapAdminPassword;
	
	private final static String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

	public LdapServiceImpl() {
		
		ldapUsersUri = Parameters.getParameterValue(LDAP_USERS_URI_PROPERTY);
		ldapGroupsUri = Parameters.getParameterValue(LDAP_GROUPS_URI_PROPERTY);
		ldapAdminDn = Parameters.getParameterValue(LDAP_ADMIN_DN_PROPERTY);
		ldapAdminPassword = Parameters.getParameterValue(LDAP_ADMIN_PASSWORD);
		
	}

	@Override
	public boolean authenticate(String username, String password) {
		
		// look for user in LDAP database
		try {
			String dn = getUserId( username );
			if (dn==null) {
				LOG.info("User: "+username+" not found");
				return false;
			} else {
				// user found - test password
				if (validate(dn, password)) {
					LOG.info("User: "+username+" authentication succedeed");
					return true;
				} else {
					LOG.info("User: "+username+" authentication failed");
					return false;
				}
			}
		} catch(Exception e) {
			LOG.warning("Error while executing LDAP authentication: "+e.getMessage());
			return false;
		}

	}

	/**
	 * Validates (dn, password) credentials versus the LDAP user database.
	 * 
	 * @param dn
	 * @param password
	 * @return
	 * @throws Exception
	 */
	protected boolean validate(String dn, String pwd) throws Exception {

		try {
			// create the initial context
			DirContext ctx = ldapContext(dn, pwd, ldapUsersUri);
			boolean result = ctx != null;
	
			if (ctx != null) {
				ctx.close();
			}
			return result;
			
		} catch(Exception e) {
			return false;
		}

	}
	

	/**
	 * Queries LDAP for a user's distinguished name,
	 * using the LDAP administrator credentials.
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private String getUserId(String username) throws Exception {
		
		DirContext ctx = ldapContext(ldapAdminDn, ldapAdminPassword, ldapUsersUri);

		String filter = "(uid=" + username + ")";
		SearchControls ctrl = new SearchControls();
		ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
		NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctrl);

		String dn;
		if (answer.hasMore()) {
			SearchResult result = (SearchResult) answer.next();
			dn = result.getNameInNamespace();
		} else {
			dn = null;
		}
		answer.close();
		ctx.close();
		
		return dn;
		
	}
	
	/**
	 * Queries LDAP for the groups a user belongs to,
	 * using the LDAP administrator credentials.
	 * 
	 * @param dn
	 * @return
	 * @throws Exception
	 */
	public List<String> authorize(String userdn) {
		
		List<String> groups = new ArrayList<String>();
		
		try {
		
		DirContext ctx = ldapContext(ldapAdminDn, ldapAdminPassword, ldapGroupsUri);

		String filter = "(&(objectClass=groupOfUniqueNames)(uniqueMember="+userdn+"))";
		SearchControls ctrl = new SearchControls();
		ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
		NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctrl);

		if (answer.hasMore()) {
			SearchResult result = (SearchResult) answer.next();
			String groupdn = result.getNameInNamespace();
			LOG.info("Found group="+groupdn);
			groups.add(groupdn);
		}
		answer.close();
		ctx.close();
		
		} catch(Exception e) {
			LOG.info("Error while querying LDAP for groups: "+e.getMessage());
		}
		
		return groups;
		
	}
	
	/**
	 * Creates an LDAP context using the given credentials.
	 * 
	 * @return
	 * @throws Exception
	 */
	private DirContext ldapContext(String dn, String pwd, String uri) throws Exception {
		
		Hashtable<String,String> env = new Hashtable <String,String>();
		
		env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
		env.put(Context.PROVIDER_URL, uri);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		
		// use the given credentials
		env.put(Context.SECURITY_PRINCIPAL, dn);
		env.put(Context.SECURITY_CREDENTIALS, pwd);
	
		return new InitialDirContext(env);
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
		String dn = self.getUserId( user );
		
		if (dn != null) {
			
			// found user - test password
			if ( self.validate( dn, password ) ) {
				LOG.info( "User '" + user + "' authentication succeeded" );
				
				// retrieve groups
				List<String> groups= self.authorize(dn);
				LOG.info("User groups="+groups);
				
			} else {
				LOG.info( "User '" + user + "' authentication failed" );
			}
			
		} else {
			LOG.info( "User '" + user + "' not found" );
		}
				
	}

}