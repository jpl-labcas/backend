package gov.nasa.jpl.labcas.data_access_api.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Handles audit logging for file downloads.
 */
public class DownloadAuditLogger {
    
    private static final Logger LOG = Logger.getLogger(DownloadAuditLogger.class.getName());
    private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    
    /**
     * Logs a download event to the audit log file.
     * 
     * @param distinguishedName The user's distinguished name
     * @param fileId The ID of the downloaded file
     */
    public static void logDownload(String distinguishedName, String fileId) {
        try {
            String now = iso8601.format(new Date());
            File downloadLog = new File(System.getenv("LABCAS_HOME"), "download.log");
            // TODO: rotation? Or just use Java Logging?
            PrintWriter writer = null;
            try {
                // true to FileWriter means append
                writer = new PrintWriter(new BufferedWriter(new FileWriter(downloadLog, true)));
                writer.println(now + ";" + distinguishedName + ";" + fileId);
            } finally {
                if (writer != null) writer.close();
            }
        } catch (IOException ex) {
            LOG.warning("Could not log this download (" + ex.getClass().getName() + ") but continuing");
            ex.printStackTrace();
            LOG.warning(ex.getMessage());
            LOG.warning("Now continuing to download the file with the download helper…");
        }
    }
} 