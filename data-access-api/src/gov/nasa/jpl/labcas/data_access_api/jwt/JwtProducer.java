package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTCreationException;

public class JwtProducer {
	
	
	public JwtProducer() {}
		
	public String getToken(String subject, String fileName) throws JWTCreationException {
		
		Date now = new Date();
		Date expires = new Date();
		expires.setTime(now.getTime()+Constants.EXPIRES_IN_SECONDS*1000);
		
	    String token = JWT.create()
	    				   .withIssuer(Constants.ISSUER)
	    				   .withIssuedAt(now)
	    				   .withNotBefore(now)
	    				   .withExpiresAt(expires)
	    				   .withSubject(subject)
	    				   .withClaim(Constants.CLAIM_FILENAME, fileName)
	                   .sign(Constants.algorithm);
	    return token;
		
	}
	
	public static void main(String[] args) {
		
		// produce token
		JwtProducer self = new JwtProducer();
		String subject = "luca";
		String fileName = "abc.txt";
		String token = self.getToken(subject, fileName);
		System.out.println("Token="+token);
		
		// consume token
		JwtConsumer other = new JwtConsumer();
		boolean tf = other.verifyToken(token);
		System.out.println("Token is valid? "+tf);
 		
	}


}