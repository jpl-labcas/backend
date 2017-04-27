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

/**
 * Filter that enforces proper authorization when downloading files from the OODT Product Server.
 * 
 * @author Luca Cinquini
 *
 */
public class AuthorizationFilter implements Filter {
	
	private FilterConfig filterConfig;
	
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
		
		// FIXME: set cookie for that productID
		final Cookie _cookie = new Cookie(Constants.COOKIE_PRODUCT_ID_NAME, productId);
		//_cookie.setSecure(true);
		_cookie.setMaxAge(Constants.COOKIE_PRODUCT_ID_LIFETIME);
		final String url = req.getRequestURL().toString();
		final URL reqURL = new URL(url);
		_cookie.setDomain(reqURL.getHost()); // cookie sent to all applications on this host
		_cookie.setPath("/");                // cookie will be sent to all pages in web application
		if (LOG.isDebugEnabled()) LOG.debug("Set cookie name="+_cookie.getName()+" value="+_cookie.getValue());
		resp.addCookie(_cookie);
		
		// retrieve cookie to check authorization
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
		      for (int i = 0; i < cookies.length; i++) {
		          Cookie cookie=cookies[i];
		          if (cookie.getName().equals(Constants.COOKIE_PRODUCT_ID_NAME)) {
		        	  if (cookie.getValue().equals(productId)) {
		        	   		// request is authorized, keep processing
		        		    if (LOG.isDebugEnabled()) LOG.debug("Found authorization cookie: name="+cookie.getName()+" value="+cookie.getValue());
		        	   		chain.doFilter(request, response);   
		        	  }
		          }
		       }
		 }
				
   		// authorization cookie was NOT found, return error
		if (LOG.isDebugEnabled()) LOG.debug("Authorization cookie for productID="+productId+" not found");
   		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sorry, you are not authorized to download this product.");

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

}
