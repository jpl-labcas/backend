// Based on https://cutt.ly/04iPQZA

package gov.nasa.jpl.edrn.labcas.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class InsecureSocketFactory implements SecureProtocolSocketFactory {
    private static final Log LOG = LogFactory.getLog(InsecureSocketFactory.class);
    private SSLContext sslContext = null;

    private static SSLContext createInsecureSSLContext() {
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] {new EasyX509TrustManager(null)}, null);
            return context;            
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new HttpClientError(ex.toString());
        }
    }

    private SSLContext getSSLContext() {
        if (this.sslContext == null)
            this.sslContext = createInsecureSSLContext();
        return this.sslContext;
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException,
        UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
        final HttpConnectionParams params
    ) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null)
            throw new IllegalArgumentException("Parameters must not be null");
        int timeout = params.getConnectionTimeout();
        SocketFactory socketFactory = getSSLContext().getSocketFactory();
        if (timeout == 0)
            return socketFactory.createSocket(host, port, localAddress, localPort);
        else {
            Socket socket = socketFactory.createSocket();
            SocketAddress localAddr = new InetSocketAddress(localAddress, localPort);
            SocketAddress remoteAddr = new InetSocketAddress(host, port);
            socket.bind(localAddr);
            socket.connect(remoteAddr, timeout);
            return socket;
        }
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(InsecureSocketFactory.class));
    }

    public int hashCode() {
        return InsecureSocketFactory.class.hashCode();
    }
}
