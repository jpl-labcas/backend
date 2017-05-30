package gov.nasa.jpl.edrn.labcas.filters;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Filter that enforces proper authorization when downloading files from the OODT Product Server.
 * 
 * @author Luca Cinquini
 *
 */
public class AuthorizationFilter implements Filter {
	
	private FilterConfig filterConfig;
	String privateKeyFilePath = null;
	private PublicKey pubKey = null;
	
	private final Log LOG = LogFactory.getLog(this.getClass());

	public AuthorizationFilter() {}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		// extract productID from HTTP request parameters
		final HttpServletRequest req = (HttpServletRequest)request;
		final HttpServletResponse resp = (HttpServletResponse)response;
		final String productId = request.getParameter(Constants.PARAMETER_PRODUCT_ID);
		if (LOG.isDebugEnabled()) LOG.debug("Establishing access control for productId="+productId);
		
		// FIXME: set cookie containing the signature for the product id
		try {
			
			PEMReader pemReader = new PEMReader(new FileReader(privateKeyFilePath));
			KeyPair keys = (KeyPair) pemReader.readObject();
			pemReader.close();
			
		    // sign the data
		    Signature sg = Signature.getInstance("SHA1withRSA");
		    sg.initSign(keys.getPrivate());
		    sg.update(productId.getBytes());
		    
		    // add cookie with signed data
		    String signature = DatatypeConverter.printBase64Binary(sg.sign());
		    
			final Cookie _cookie = new Cookie(Constants.COOKIE_PRODUCT_ID_SIGNATURE, signature);
			//_cookie.setSecure(true);
			_cookie.setMaxAge(Constants.COOKIE_PRODUCT_ID_LIFETIME);
			final String url = req.getRequestURL().toString();
			final URL reqURL = new URL(url);
			_cookie.setDomain(reqURL.getHost()); // cookie sent to all applications on this host
			_cookie.setPath("/");                // cookie will be sent to all pages in web application
			if (LOG.isDebugEnabled()) LOG.debug("Set cookie name="+_cookie.getName()+" value="+_cookie.getValue());
			resp.addCookie(_cookie);
		
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
		
		// retrieve cookie to check authorization
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
		      for (int i = 0; i < cookies.length; i++) {
		          Cookie cookie=cookies[i];
		          if (cookie.getName().equals(Constants.COOKIE_PRODUCT_ID_SIGNATURE)) {
		        	  
		        	  if (LOG.isDebugEnabled()) LOG.debug("Found authorization cookie: name="+cookie.getName()+" value="+cookie.getValue());
		        	  
		        	  try {
		        		  
		        		  // load public key
		        		  Signature sg = Signature.getInstance("SHA1withRSA");
		        		  sg.initVerify(pubKey);
		        		  
		        		  // read product id into signature instance
		        		  sg.update(productId.getBytes());
		        		  
		        		  // validate signature
		        		  if (sg.verify(DatatypeConverter.parseBase64Binary(cookie.getValue()))) {
		        			  
		        			  if (LOG.isDebugEnabled()) LOG.debug("Cookie signature is valid");
		        			  // request is authorized, keep processing
		        			  chain.doFilter(request, response);
		        		    	
		        		  } else {
		        		      if (LOG.isWarnEnabled()) LOG.warn("Cookie signature is invalid");
		        		  }
		        		  
		        		  
		        	  } catch(SignatureException e1) {
		        		LOG.error(e1.getMessage());
		        	  } catch(NoSuchAlgorithmException e2) {
		      			LOG.error(e2.getMessage());
			      	  } catch(InvalidKeyException e3) {
			      		LOG.error(e3.getMessage());
			      	  }

		          }
		       }
		 }
				
   		// authorization cookie was NOT found, or signature validation failed
		if (LOG.isDebugEnabled()) LOG.debug("Authorization failed for productID="+productId);
   		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sorry, you are not authorized to download this product.");

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		// read private key location from filter configuration
		this.filterConfig = filterConfig;
		this.privateKeyFilePath = filterConfig.getInitParameter("privateKeyFilePath");
		LOG.info("Using private key file: "+privateKeyFilePath);
		
	    // read private key into memory
		try {
		    Security.addProvider(new BouncyCastleProvider());
		    PEMReader pemReader = new PEMReader(new FileReader(privateKeyFilePath));
		    this.pubKey = ((KeyPair) pemReader.readObject()).getPublic();
		    pemReader.close();
		    		    
		} catch(IOException e1) {
			LOG.error(e1.getMessage());
		} 
	}

}
