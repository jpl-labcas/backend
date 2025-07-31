package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import gov.nasa.jpl.labcas.data_access_api.utils.Parameters;

/**
 * Handles integration with Zipperlab service for creating ZIP files.
 */
public class ZipperlabService {
    
    private static final Logger LOG = Logger.getLogger(ZipperlabService.class.getName());
    
    /**
     * Initiates a ZIP creation request with Zipperlab.
     * 
     * @param email The user's email address
     * @param files List of file paths to include in the ZIP
     * @return The UUID of the ZIP request
     * @throws IOException if the request fails
     */
    public static String initiateZIP(String email, List<String> files) throws IOException {
        LOG.info("👀 initiateZIP for " + email + " and files " + files);

        // Newer Javas include JSON support directly; we're stuck on 1.8 so we assemble by hand:
        StringBuilder jsonPayload = new StringBuilder();
        jsonPayload.append("{");
        jsonPayload.append("\"operation\":\"initiate\",");
        jsonPayload.append("\"email\":\"").append(email).append("\",");
        jsonPayload.append("\"files\":[");
        for (int i = 0; i < files.size(); ++i) {
            jsonPayload.append("\"").append(files.get(i)).append("\"");
            if (i < files.size() - 1) jsonPayload.append(",");
        }
        jsonPayload.append("]}");
        LOG.info("👀 POST data to Zipperlab is: «" + jsonPayload + "»");
        byte[] postData = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);

        String stringURL = Parameters.getParameterValue("zipperlab");
        URL url = new URL(stringURL);
        LOG.info("👀 Zipperlab URL is " + url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.write(postData);
        dataOutputStream.flush();
        dataOutputStream.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            LOG.info("👀 got OK status, so reading the UUID");
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String uuid = in.readLine();
                LOG.info("👀 Got UUID " + uuid);
                return uuid;
            } finally {
                if (in != null) in.close();
            }
        } else {
            LOG.info("🚨 Got HTTP status " + connection.getResponseCode());
            throw new IOException("Request failed with HTTP status " + connection.getResponseCode());
        }
    }
} 