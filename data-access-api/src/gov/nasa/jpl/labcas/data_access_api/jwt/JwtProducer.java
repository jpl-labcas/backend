package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.Date;
import java.util.logging.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Class that generates Json Web Tokens carrying the user identity.
 */
public class JwtProducer {
	
	private final static Logger LOG = Logger.getLogger(JwtProducer.class.getName());
	
	public JwtProducer() {}
		
	public String getToken(String subject) throws JWTCreationException {
		
		Date now = new Date();
		Date expires = new Date();
		expires.setTime(now.getTime()+Constants.EXPIRES_IN_SECONDS*1000);
		
		// JWT tokens have an expiration date
		// which will automatically be used for validation
	    String token = JWT.create()
	    				   .withIssuer(Constants.ISSUER)
	    				   .withAudience(Constants.ISSUER)
	    				   .withIssuedAt(now)
	    				   .withNotBefore(now)
	    				   .withExpiresAt(expires)
	    				   .withSubject(subject)
	    				   //.withClaim(Constants.CLAIM_FILENAME, fileName)
	                   .sign(Constants.algorithm);
	    return token;
		
	}
	
	public static void main(String[] args) {
		
		// produce token
		JwtProducer self = new JwtProducer();
		String subject = "uid=lcinquini,ou=users,o=MCL";
		String token = self.getToken(subject);
		
		// pause
		try {
		    Thread.sleep(1000);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		// consume token
		JwtConsumer other = new JwtConsumer();
		DecodedJWT jwt = other.verifyToken(token);
		LOG.info("Subject="+jwt.getSubject());
 		
	}


}