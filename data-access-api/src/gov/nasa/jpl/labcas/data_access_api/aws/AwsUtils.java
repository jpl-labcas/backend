package gov.nasa.jpl.labcas.data_access_api.aws;

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
	 * Returns the S3 staging bucket name (without the 's3//' prefix).
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getStagingBucketName() throws Exception {
		
		return _getBucketName("STAGING_AREA");
		
	}
	
	/**
	 * Returns the S3 archive bucket name (without the 's3//' prefix).
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getArchiveBucketName() throws Exception {
		
		return _getBucketName("ARCHIVE_AREA");
		
	}
		
	/**
	 * Retrieves a bucket name from the environment
	 * and removes the 's3://' prefix.
	 * @param bucketEnvVariable
	 * @return
	 * @throws Exception
	 */
	private static String _getBucketName(String bucketEnvVariable) throws Exception {
		
		String bucket = System.getenv(bucketEnvVariable);
		if (bucket==null) {
			throw new Exception("Env variable " + bucketEnvVariable + " not found");
		} else {
			bucket = bucket.substring(5);
		}
		
		return bucket;
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