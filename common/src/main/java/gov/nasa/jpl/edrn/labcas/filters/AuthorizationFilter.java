package gov.nasa.jpl.edrn.labcas.filters;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.RsaUtils;

/**
 * Filter that enforces proper authorization when downloading files from the OODT Product Server.
 * 
 * @author Luca Cinquini
 *
 */
public class AuthorizationFilter implements Filter {
	
	private FilterConfig filterConfig;
	RsaUtils rsaUtils;
	
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
		
		// FIXME: set the cookie with signature
		try {
					    
		    // add cookie with signed data
		    String signature = rsaUtils.sign(productId);
		    
			final Cookie _cookie = new Cookie(Constants.COOKIE_PRODUCT_ID_NAME, signature);
			_cookie.setSecure(true); 
			_cookie.setMaxAge(Constants.COOKIE_PRODUCT_ID_LIFETIME);
			final String url = req.getRequestURL().toString();
			final URL reqURL = new URL(url);
			_cookie.setDomain(reqURL.getHost()); // cookie sent to all applications on this host
			_cookie.setPath("/");                // cookie will be sent to all pages in web application
			if (LOG.isDebugEnabled()) LOG.debug("Set cookie name="+_cookie.getName()+" value="+_cookie.getValue()
			                                   +" domain="+_cookie.getDomain()+" path="+_cookie.getPath()+" max age="+_cookie.getMaxAge());
			resp.addCookie(_cookie);
		
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}

		
		// retrieve cookie to check authorization
		boolean authorized = false;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
		      for (int i = 0; i < cookies.length; i++) {
		          Cookie cookie=cookies[i];
		          if (LOG.isInfoEnabled()) LOG.info("Found cookie="+cookie.getName()+" value="+cookie.getValue());
		          if (cookie.getName().equals(Constants.COOKIE_PRODUCT_ID_NAME)) {
		        	  
		        	  if (LOG.isInfoEnabled()) LOG.info("Found authorization cookie: name="+cookie.getName()+" value="+cookie.getValue());
		        	  		        		  
		        		  // validate signature
		        		  if (rsaUtils.verify(productId, cookie.getValue())) {
		        			  if (LOG.isDebugEnabled()) LOG.debug("Cookie signature is valid");
		        		      authorized = true;
		        		  } else {
		        		      if (LOG.isWarnEnabled()) LOG.warn("Cookie signature is NOT valid");
		        		  }
		        		  
		          }
		       }
		 }
				
		if (authorized) {
			  // request is authorized, keep processing
			  chain.doFilter(req, resp);
		} else {
			// authorization cookie was NOT found, or signature validation failed
			if (LOG.isDebugEnabled()) LOG.debug("Authorization failed for productID="+productId);
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sorry, you are not authorized to download this product.");
		}

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		// read private key location from filter configuration
		this.filterConfig = filterConfig;
		
		// create re-usable signing utility
		String privateKeyFilePath = filterConfig.getInitParameter("privateKeyFilePath");
		LOG.info("Using private key file: "+privateKeyFilePath);
		rsaUtils = new RsaUtils(privateKeyFilePath);
		
	}

}
