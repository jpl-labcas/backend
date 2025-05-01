package gov.nasa.jpl.labcas.data_access_api.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * The InsecureLDAPFactory exists because JPL uses some truly bizarre certificate chains.
 * 
 * Worse, they change yearly.
 */
public class InsecureLDAPFactory extends SSLSocketFactory {
	private SSLSocketFactory factory;
	public InsecureLDAPFactory() throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        factory = ctx.getSocketFactory();
	}
    public static SocketFactory getDefault() {
        try {
            return new InsecureLDAPFactory();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create InsecureLDAPFactory", ex);
        }
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return factory.createSocket(socket, host, port, autoClose);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return factory.createSocket(host, port);
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return factory.createSocket(host, port, localHost, localPort);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return factory.createSocket(host, port);
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return factory.createSocket(address, port, localAddress, localPort);
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}
