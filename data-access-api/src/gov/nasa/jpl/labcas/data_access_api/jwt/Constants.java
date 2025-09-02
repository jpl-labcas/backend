package gov.nasa.jpl.labcas.data_access_api.jwt;

public class Constants {
	
	// FIXME: must read password from protected property file
	//public static Algorithm algorithm = Algorithm.HMAC256("secret");
	
	public static String JWT_SECRET_KEY = "jwtSecret";
	
	public static String ISSUER = "LabCAS";
	
	public static String AUDIENCE = "LabCAS";
	
	public static String SESSION_ID = "Shubhneek";  // "Shubhneek" means "session identifier"
	
	public static int EXPIRES_IN_SECONDS = 3600;  // 1 hour

}
