package gov.nasa.jpl.labcas.data_access_api.filter;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TimeZone;
import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import javax.net.SocketFactory;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.NamingEnumeration;

/**
 * Implementation of @see UserService that queries an LDAP database.
 * 
 * @author Luca Cinquini
 *
 */
public class UserServiceLdapImpl implements UserService {
	private static final Date UNIX_EPOCH = new Date(0L);
	private static final SimpleDateFormat LDAP_DATE_FORMAT;

	static {
		LDAP_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
		LDAP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final static Logger LOG = Logger.getLogger(UserServiceLdapImpl.class.getName());

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

	public UserServiceLdapImpl() {
		
		ldapUsersUri = Parameters.getParameterValue(LDAP_USERS_URI_PROPERTY);
		ldapGroupsUri = Parameters.getParameterValue(LDAP_GROUPS_URI_PROPERTY);
		ldapAdminDn = Parameters.getParameterValue(LDAP_ADMIN_DN_PROPERTY);
		ldapAdminPassword = Parameters.getParameterValue(LDAP_ADMIN_PASSWORD);
		
	}

	/**
	 * Authenticates a user by binding to LDAP with the given username and password.
	 * Returns the user DN if the credentials are valid.
	 */
	@Override
	public String getValidUser(String username, String password) {
		
		String dn = null; // i.e. authentication = false
		
		// look for user in LDAP database
		try {
			dn = getUserId( username );
			if (dn==null) {
				LOG.info("User: "+username+" not found");
			} else {
				// user found - test password
				if (validateUserCredentials(dn, password)) {
					LOG.info("User: "+username+" authentication succedeed");
				} else {
					LOG.info("User: "+username+" authentication failed");
					return null; // IMPORTANT: must return NULL, not the dn
				}
			}
		} catch(Exception e) {
			LOG.warning("Error while executing LDAP authentication: "+e.getMessage());
		}
		
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
	@Override
	public List<String> getUserGroups(String userdn) {
		
		List<String> groups = new ArrayList<String>();
		
		try {
		
			DirContext ctx = ldapContext(ldapAdminDn, ldapAdminPassword, ldapGroupsUri);
	
			String filter = "(&(objectClass=groupOfUniqueNames)(uniqueMember="+userdn+"))";
			SearchControls ctrl = new SearchControls();
			ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctrl);
	
			while (answer.hasMore()) {
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
	 * Queries LDAP for a user's distinguished name,
	 * using the LDAP administrator credentials.
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private String getUserId(String username) throws Exception {
		LOG.warning("🚔 making dir context with dn=" + ldapAdminDn + " and ldapUsersUri=" + ldapUsersUri);
		DirContext ctx = null;
		try {
			ctx = ldapContext(ldapAdminDn, ldapAdminPassword, ldapUsersUri);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "🚔🚔🚔 OH POOP: ", ex);
		}

		String filter = "(uid=" + username + ")";
		SearchControls ctrl = new SearchControls();
		ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
		LOG.warning("🚔 searching on ctx with filter=" + filter + ", ctrl=" + ctrl);
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
		LOG.warning("🚔 returning dn=" + dn);
		return dn;
		
	}
	
	/**
	 * Validates (dn, password) credentials versus the LDAP user database.
	 * 
	 * @param dn
	 * @param password
	 * @return
	 * @throws Exception
	 */
	private boolean validateUserCredentials(String dn, String pwd) throws Exception {

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
		// JPL-issued certificates are dodgy
		env.put("java.naming.ldap.factory.socket", "gov.nasa.jpl.labcas.data_access_api.filter.InsecureLDAPFactory");
		
		// use the given credentials
		env.put(Context.SECURITY_PRINCIPAL, dn);
		env.put(Context.SECURITY_CREDENTIALS, pwd);
	
		return new InitialDirContext(env);
	}


	/**
	 * Convert an LDAP timestamp string `YYYYMMDDHHMMSSZ` to a Date.
	 *
	 * @param ts timestamp
	 * @return a Date of ts
	 */
	private Date convertTStoDate(String ts) {
        try {
            return LDAP_DATE_FORMAT.parse(ts);
        } catch (ParseException ex) {
			LOG.info("🕰️ Cannot convert «" + ts + "» to Date: " + ex + "; returnin Unix epoch");
			return UNIX_EPOCH;
        }
	}

	/**
	 * Get the "last modified" timestamp for the user with the given userDN
	 * 
	 * @return a Date
	 */
	public Date getModificationTime(String userDN) {
		try {
			LdapName ldapName = new LdapName(userDN);
			Rdn relativeDN = ldapName.getRdn(ldapName.size() - 1);
			LOG.info("🤔 userDN " + userDN + " gives ldapName " + ldapName + " and relativeDN is " + relativeDN);
			DirContext ctx = ldapContext(ldapAdminDn, ldapAdminPassword, ldapUsersUri);
			String[] attrIDs = {"modifyTimestamp"};
			Attributes attrs = ctx.getAttributes(relativeDN.toString(), attrIDs);
			Attribute modifyTimestamp = attrs.get("modifyTimestamp");
			if (modifyTimestamp != null) {
				String ts = (String) modifyTimestamp.get();
				LOG.info("🪢 String version of attribute is «" + ts + "»");
				return convertTStoDate(ts);
			} else {
				LOG.info("🤷 No modifyTimestamp for " + relativeDN + " so returning Unix epoch");
				return UNIX_EPOCH;
			}
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			LOG.info("😬 Exception «" + ex + "» while getting modification time for " + userDN
				+ " using context at URI " + ldapUsersUri + "; so returning Unix epoch");
			return UNIX_EPOCH;
		}
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
		UserServiceLdapImpl self = new UserServiceLdapImpl();
		String dn = self.getUserId( user );
		
		if (dn != null) {
			
			// found user - test password
			if ( self.validateUserCredentials( dn, password ) ) {
				
				// retrieve groups
				List<String> groups= self.getUserGroups(dn);

				LOG.info("🧑‍🔬 User «" + user + "» authentication succeeded");
				LOG.info("🧑‍🧑‍🧒‍🧒 User groups = " + groups);
				
			} else {
				LOG.info( "User '" + user + "' authentication failed" );
			}
			
		} else {
			LOG.info( "User '" + user + "' not found" );
		}
				
	}

}