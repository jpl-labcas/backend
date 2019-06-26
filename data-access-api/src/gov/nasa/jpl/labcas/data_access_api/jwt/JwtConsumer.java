package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.logging.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

/**
 * Class to verify and decode a Json Web Token.
 */
public class JwtConsumer {
		
	private final static Logger LOG = Logger.getLogger(JwtConsumer.class.getName());
	
	// reusable instance
	private JWTVerifier verifier;
	
	public JwtConsumer() {
		
		String secret = Parameters.getParameterValue(Constants.JWT_SECRET_KEY);
		Algorithm algorithm = Algorithm.HMAC256(secret);
		
		// the ISSUER and AUDIENCE fields are required
		// otherwise the token is invalid
		// also tokens are automatically checked for expiration
		verifier = JWT.require(algorithm)
				.withIssuer(Constants.ISSUER)
				.withAudience(Constants.AUDIENCE)
				.build();
	
	}
	
	public DecodedJWT verifyToken(String token) throws JWTVerificationException  {
		
	    DecodedJWT jwt = verifier.verify(token);
	    LOG.info("Subject="+jwt.getSubject());
	    //LOG.info("Filename="+jwt.getClaim(Constants.CLAIM_FILENAME).asString());
	    return jwt;
		   
	}
	

}
