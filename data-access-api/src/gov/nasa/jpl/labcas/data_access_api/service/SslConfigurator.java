package gov.nasa.jpl.labcas.data_access_api.service;

import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.util.logging.Logger;

/**
 * Configures SSL to trust all certificates for internal service communication.
 * This is needed because services on edrn-docker use self-signed certificates.
 */
public class SslConfigurator {
    
    private static final Logger LOG = Logger.getLogger(SslConfigurator.class.getName());
    
    static {
        configureSsl();
    }
    
    /**
     * Configures SSL to trust all certificates for internal service communication.
     */
    public static void configureSsl() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.severe("💣 Cannot install all-trusting trust manager for SSL; aborting");
            System.err.println("💣 Cannot install all-trusting trust manager for SSL; aborting");
            System.exit(-1);
        }
    }
} 