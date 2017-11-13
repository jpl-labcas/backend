package gov.nasa.jpl.edrn.labcas.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

public class OhifTest {

	public static void main(String[] args) {
		
		String filepath = "/Users/cinquini/Desktop/EDRN_DATA/CBIS-DDSM/Mass-Training_Full_Mammogram_Images/1/";
		String filename = "1.3.6.1.4.1.9590.100.1.2.97080556210372043520897273802597422083.dcm";
		File file = new File(filepath + filename);
		
		String serverUrl = "http://localhost:8042/instances";
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpEntity resEntity = null;
		
		//Logger LOG = Logger.getLogger(OhifTest.class.getName());
		
		try {
			
			HttpPost httppost = new HttpPost(serverUrl);

			FileBody upload = new FileBody(file, filename, "application/octet-stream", "UTF-8");
			            
			//StringBody case_id = new StringBody(filename);

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("upload", upload);
			//reqEntity.addPart("case_id", case_id);
			httppost.setEntity(reqEntity);
			
			httppost.addHeader("Expect:", null);
			
			byte[] encodedBytes = Base64.encodeBase64("orthanc:orthanc".getBytes());
			httppost.setHeader("Authorization", "Basic " + new String(encodedBytes));

			HttpResponse response = httpclient.execute(httppost);
			resEntity = response.getEntity();
			System.out.println("Upload result="+resEntity.toString());
			
		} catch(Exception e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (resEntity != null) {
					InputStream instream = resEntity.getContent();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
		            String line = null;
		            while ((line = reader.readLine()) != null) {
		                System.out.println(line);
		            }
		            reader.close();
					instream.close();
				}
			} catch(Exception e) {}
		}

		
		
		

	}

}
