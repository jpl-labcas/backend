package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;


/**
 * Simple class to execute an HTTP GET/POST request.
 * This class is a wrapper around the Apache HTTP client.
 */
public class HttpClient {
	
	private final static Logger LOG = Logger.getLogger(HttpClient.class.getName());

	/**
	 * Method to execute an HTTP GET request.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Response doGet(final String url) {

		CloseableHttpClient httpclient = null;
		try {
			httpclient = HttpClients.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			System.err.println("I give up in HttpClient.doGet");
			System.exit(42);
		}

		LOG.info("HTTP request: " + url);
		HttpGet httpGet = new HttpGet(url);
			
		try {
			CloseableHttpResponse response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			// return the same response to the client
			String content = IOUtils.toString(entity.getContent(), "UTF-8");
			LOG.info("Response status: "+response.getStatusLine().getStatusCode());
			LOG.info("Response content: "+content);
			return Response.status(response.getStatusLine().getStatusCode()).entity(content).build();

		} catch (Exception e) {
			
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning("HTTP Get response error: "+e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			
		} finally {
			try {
				httpclient.close();
			} catch(IOException e) {
				LOG.warning(e.getMessage());
			}
		}

	}

	/**
	 * Method to send a POST request to Solr.
	 * 
	 * @param url: the URL to post the request to - without any additional HTTP parameters
	 * @param data: the data to be posted - possibly an XML or JSON document
	 * @param contentType: "text/xml", "application/json", etc.
	 *         
	 * @return
	 * @throws IOException
	 */
	public Response doPost(final String url, final String data, String contentType) {
		
			
		LOG.info("Posting to url="+url+" data="+data);
		CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost(url);
		    
		try {
			
		    StringEntity payload = new StringEntity(data);
		    httpPost.setEntity(payload);
		    httpPost.setHeader("Accept", contentType);
		    httpPost.setHeader("Content-type", contentType);
		 		 
		    CloseableHttpResponse response = client.execute(httpPost);
		    HttpEntity entity = response.getEntity();

			// return the same response to the client
			String content = IOUtils.toString(entity.getContent(), "UTF-8");
			LOG.info("Response status: "+response.getStatusLine().getStatusCode());
			LOG.info("Response content: "+content);
			return Response.status(response.getStatusLine().getStatusCode()).entity(content).build();
	    
		} catch(Exception e) {
			
			// send 500 "Internal Server Error" response
			e.printStackTrace();
			LOG.warning("HTTP Post response error: "+e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			
		} finally {
			try {
				client.close();
			} catch(IOException e) {
				LOG.warning(e.getMessage());
			}
		}

	}

}
