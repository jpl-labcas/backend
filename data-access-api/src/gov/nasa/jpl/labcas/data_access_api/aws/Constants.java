package gov.nasa.jpl.labcas.data_access_api.aws;

/**
 * Interface containing parameters for AWS services.
 *
 */
public interface Constants {
	
	public static String AWS_DEFAULT_REGION = "us-west-2";
	public static String AWS_DEFAULT_PROFILE = "default";
	
	// data download parameters
	public static int AWS_DOWNLOAD_URL_EXPIRATION_TIME_SECS_DEFAULT= 20;
	
}