package gov.nasa.jpl.labcas.data_access_api.aws;

import java.util.Arrays;

/**
 * Common utilities to handle AWS configuration parameters.
 *
 */
public class AwsUtils {
	
	/**
	 * Retrieves the name of a profile from the environment,
	 * or the default profile name.
	 * 
	 * @param profileName
	 * @return
	 */
	public static String getProfile(String profileName) {
		
		String profile = System.getenv(profileName);
		if (profile==null) {
			profile = Constants.AWS_DEFAULT_PROFILE;
		}
		
		return profile;
		
	}
	
	/**
	 * Returns the AWS region from the environment,
	 * or the default region.
	 * @return
	 */
	public static String getRegion() {
		
		String awsRegion = System.getenv("AWS_REGION");
		if (awsRegion==null) {
			awsRegion = Constants.AWS_DEFAULT_REGION;
		}
		
		return awsRegion;

	}
	
	/**
	 * Returns the S3 archive bucket name from the environment.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getArchiveBucketName() throws Exception {
		
		String bucket = System.getenv("S3_BUCKET");
		if (bucket==null) {
			throw new Exception("Env variable 'S3_BUCKET' is not set");
		} else {
			return bucket;
		}
		
	}
	
	/**
	 * Extract the S3 key from a full filepath of the form:
	 * s3://<bucket>/<s3key>...
	 * @param filePath
	 * @return
	 */
	public static String getS3key(String filePath) {
		
		String[] parts = filePath.split("/");
		String[] _parts = Arrays.copyOfRange(parts, 3, parts.length);
		return String.join("/", _parts);
		
	}
	
	/**
	 * Returns an integer value from the environment,
	 * or a default value if the environment variable is not found.
	 * @param envVariable
	 * @param defaultValue
	 * @return
	 */
	public static int getIntValueFromEnv(String envVariable, int defaultValue) {
		
		String envVariableValue = System.getenv(envVariable);
		if (envVariableValue!=null) {
			return Integer.parseInt(envVariableValue);
		} else {
			return defaultValue;
		}
		
	}	
	
}