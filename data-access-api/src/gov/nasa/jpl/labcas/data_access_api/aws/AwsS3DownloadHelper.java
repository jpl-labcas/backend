package gov.nasa.jpl.labcas.data_access_api.aws;

import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

/**
 * Utility class that generates pre-signed, time-limited URLs
 * for downloading files from AWS S3.
 */
public class AwsS3DownloadHelper {
		
	// class that generates the URLs using Amazon custom scheme
	AmazonS3 s3Client = null;
	
	// will only generate URLs for this bucket
	String bucket = null;
	
	// number of seconds a URL will remain valid
	int urlExpirationTimeSecs = 0;
	
	private final static Logger LOG = Logger.getLogger(AwsS3DownloadHelper.class.getName());
	
	/**
	 * Constructor reads AWS parameters from environmental variables.
	 */
	public AwsS3DownloadHelper() throws Exception {
		
		String profile = AwsUtils.getProfile("AWS_S3_READONLY_PROFILE");		
		String awsRegion = AwsUtils.getRegion();
		bucket = AwsUtils.getArchiveBucketName();
		urlExpirationTimeSecs = AwsUtils.getIntValueFromEnv(
				"AWS_DOWNLOAD_URL_EXPIRATION_TIME_SECS", Constants.AWS_DOWNLOAD_URL_EXPIRATION_TIME_SECS_DEFAULT);
		
		// create S3 client to generate the URLs
        s3Client = AmazonS3ClientBuilder.standard()
        		                        .withRegion(awsRegion)
        		                        .withCredentials(new ProfileCredentialsProvider(profile))
        		                        .build();
        LOG.info("Created Amazon S3 client with profile="+profile+", region="+awsRegion
          		+" generating URLs for bucket="+bucket+" with validity="+urlExpirationTimeSecs+" secs");
        
	}
	
	public URL getUrl(String objectKey, String versionId) {        

        try {   
    
            // set the pre-signed URL to expire after EXPIRE_TIME_MILLIS
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime() + urlExpirationTimeSecs*1000; // must add milliseconds
            expiration.setTime(expTimeMillis);

            // generate the pre-signed URL.
            LOG.info("Generating pre-signed URL for objectKey="+objectKey+" versionId="+versionId);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = 
                    new GeneratePresignedUrlRequest(bucket, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            // download a specific version
            if (versionId!=null) {
            		generatePresignedUrlRequest.setVersionId(versionId);
            }
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    
            LOG.info("Pre-Signed URL: " + url.toString());
            return url;
            
        } catch(AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
            e.printStackTrace();
            throw e;
            
        } catch(SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
            throw e;
            
        }
		
	}
	
	public static void main(String[] args) throws Exception {
		
		AwsS3DownloadHelper self = new AwsS3DownloadHelper();
		
		String key = "Pre_Cancer_Atlas/Smart-3Seq/mdanderson/bam/AM00-Ac_1.bam.bai";
		
		URL url = self.getUrl(key, null);
		
		LOG.info("URL="+url.toString());
		
	}

}