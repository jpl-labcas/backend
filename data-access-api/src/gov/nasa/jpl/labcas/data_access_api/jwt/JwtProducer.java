package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.Date;
import java.util.logging.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

/**
 * Class that generates Json Web Tokens carrying the user identity.
 */
public class JwtProducer {
	
	// the algorithm that will be used to sign the JWT tokens
	private final Algorithm algorithm;
	
	private final static Logger LOG = Logger.getLogger(JwtProducer.class.getName());
	
	public JwtProducer() {
		
		String secret = Parameters.getParameterValue(Constants.JWT_SECRET_KEY);
		algorithm = Algorithm.HMAC256(secret);
		
	}
		
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
	                   .sign(algorithm);
	    return token;
		
	}
	
	public static void main(String[] args) {
		
		// produce token
		JwtProducer self = new JwtProducer();
		// for querying MCL
		String subject = "uid=lcinquini,ou=users,o=MCL";
		// for querying EDRN
		//String subject = "uid=luca,dc=edrn,dc=jpl,dc=nasa,dc=gov";
		// for download from labcas-dev
		//String subject = "bac2152f-f013-450a-b1ae-89e24720dbdf";
		// for download from mcl-labcas
		//String subject = "66219c4a-d4d0-41ad-a120-ca392f4f218d";
		// for download from edrn-labcas
		//String subject = "907f3954-b5ea-45a2-9398-9cad2bff85ad";
		// for download from aws-mcl
		//String subject="6bd1a5f6-02ad-4cf3-8b5e-b7212a6d9fd3";
		String token = self.getToken(subject);
		LOG.info("Token="+token);
		
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