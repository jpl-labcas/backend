package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

public class AuthenticationService {
	
	private final static Logger LOG = Logger.getLogger(AuthenticationFilter.class.getName());
	
	public boolean authenticate(String authCredentials) {

		if (null == authCredentials) return false;
		
		// header value format will be "Basic encodedstring" for Basic authentication. 
		// Example "Basic YWRtaW46YWRtaW4="
		final String encodedUserPassword = authCredentials.replaceFirst("Basic" + " ", "");
		String usernameAndPassword = null;
		try {
			byte[] decodedBytes = Base64.decodeBase64(encodedUserPassword.getBytes());
			usernameAndPassword = new String(decodedBytes, "UTF-8");
			// FIXME
			LOG.info("Username and password= "+usernameAndPassword);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
		final String username = tokenizer.nextToken();
		final String password = tokenizer.nextToken();

		// FIXME
		boolean authenticationStatus = "admin".equals(username) && "changeit".equals(password);
		return authenticationStatus;
	}
}