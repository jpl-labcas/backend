package gov.nasa.jpl.labcas.data_access_api.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.JWT;
import gov.nasa.jpl.labcas.data_access_api.filter.UserServiceLdapImpl;
import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class to verify and decode a Json Web Token.
 */
public class JwtConsumer {
		
	private final static Logger LOG = Logger.getLogger(JwtConsumer.class.getName());
	
	// reusable instance
	private JWTVerifier verifier;
	
	public JwtConsumer() {

		// For development, it may be useful to accept any JWT. Note this is highly insecure
		// and the ACCEPT_ANY_JWT=DANGEROUS environment variable must be set for this to be
		// enabled. You will also get noisy logging when this happens, because this must never
		// happen in production.

		String acceptAnyJWT = System.getenv("ACCEPT_ANY_JWT");
		if ("DANGEROUS".equals(acceptAnyJWT)) {
			verifier = new JWTVerifier() {
				@Override
				public DecodedJWT verify(String token) throws JWTVerificationException {
					LOG.warning("🚨 BYPASSING JWT VERIFICATION given a string token");
					return JWT.decode(token);
				}
				@Override
				public DecodedJWT verify(DecodedJWT jwt) throws JWTVerificationException {
					LOG.warning("🚨 IDENTITY JWT return given a decoded jwt");
					return jwt;
				}
			};
		} else {
			String secret = Parameters.getParameterValue(Constants.JWT_SECRET_KEY);
			Algorithm algorithm = Algorithm.HMAC256(secret);

			// the ISSUER and AUDIENCE fields are required
			// otherwise the token is invalid
			// also tokens are automatically checked for expiration
			verifier = JWT.require(algorithm).withIssuer(Constants.ISSUER).withAudience(Constants.AUDIENCE)
				.acceptLeeway(60).build();
		}
	
	}
	
	public DecodedJWT verifyToken(String token) throws JWTVerificationException  {
	    DecodedJWT jwt = verifier.verify(token);
		String subject = jwt.getSubject();
		String sessionID = jwt.getClaim(Constants.SESSION_ID).asString();
		LOG.info("💆 Verifying: Subject=" + subject + ", session ID = " + sessionID);

		if (!Sessions.INSTANCE.isSessionValid(sessionID, subject)) {
			LOG.severe("💆🚨 Session is invalid for " + subject + " with ID " + sessionID);
			throw new JWTVerificationException("Session is invalid");
		}
	    return jwt;
	}
	
	public static void main(String[] argv) throws Throwable {
		if (argv.length != 1) {
			System.err.println("Usage: JWTFILE");
			System.exit(-1);
		}
		JwtConsumer consumer = new JwtConsumer();
		String jwt = new String(Files.readAllBytes(Paths.get(argv[0])));
		DecodedJWT decoded = consumer.verifyToken(jwt);
		UserServiceLdapImpl ldap = new UserServiceLdapImpl();
	
		String dn = decoded.getSubject();
		List<String> groups = ldap.getUserGroups(dn);
		Date modified = ldap.getModificationTime(dn);
		Date issued = decoded.getIssuedAt();
		boolean changed = modified.after(issued);

		System.out.println("🔑 raw jwt = " + jwt);
		System.out.println("🧑 Subject DN = " + dn);
		System.out.println("📅 Issued at = " + issued);
		System.out.println("🔍 It is a = " + decoded.getIssuedAt().getClass().getName());
		System.out.println("🕰️ LDAP last modified = " + modified);
		System.out.println("🔍 It is a = " + modified.getClass().getName());

		System.out.println("👥 LDAP Groups count = " + groups.size());
		for (String group: groups)
			System.out.println("\t👥 Group: " + group);

		if (changed)
			System.out.println("🚨 LDAP entry changed after JWT issued, consider JWT invalid!");
		else
			System.out.println("😌 LDAP entry last changed before JWT issued, so consider JWT fine.");
	}
}
