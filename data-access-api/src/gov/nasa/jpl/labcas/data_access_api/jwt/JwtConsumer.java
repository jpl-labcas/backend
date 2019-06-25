package gov.nasa.jpl.labcas.data_access_api.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

public class JwtConsumer {
	
	// reusable instance
	private JWTVerifier verifier;
	
	public JwtConsumer() {
		
		verifier = JWT.require(Constants.algorithm).withIssuer(Constants.ISSUER).build();
	
	}
	
	public boolean verifyToken(String token)  {
		
		try {
			
			    DecodedJWT jwt = verifier.verify(token);
			    System.out.println("Subject="+jwt.getSubject());
			    System.out.println("Filename="+jwt.getClaim(Constants.CLAIM_FILENAME).asString());
			    System.out.println(jwt.toString());
			    
			    return true;
			    
		} catch(JWTVerificationException e) {
			   return false;
		}
		   
	}
	
	public static void main(String[] args) {
		
		String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsdWNhIiwiaXNzIjoiYXV0aDAifQ.Ms81CwW4i4QZ01CMAgplO4YlCZJMK79GQ5y7FyQmUw4";
		JwtConsumer self = new JwtConsumer();
		boolean tf = self.verifyToken(token);
		System.out.println("TF="+tf);
	}

}
