package gov.nasa.jpl.labcas.data_access_api.jwt;

import com.auth0.jwt.algorithms.Algorithm;

public class Constants {
	
	// FIXME: must read password from protected property file
	//public static Algorithm algorithm = Algorithm.HMAC256("secret");
	
	public static String JWT_SECRET_KEY = "jwtSecret";
	
	public static String ISSUER = "LabCAS";
	
	public static String AUDIENCE = "LabCAS";
	
	//public static String CLAIM_FILENAME = "FileName";
	
	// FIXME
	public static int EXPIRES_IN_SECONDS = 600;

}
