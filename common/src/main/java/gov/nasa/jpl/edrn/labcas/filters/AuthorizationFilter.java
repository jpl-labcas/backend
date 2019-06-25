package gov.nasa.jpl.edrn.labcas.filters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

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
	private RsaUtils rsaUtils;
	private JWTVerifier verifier;
	
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
		if (LOG.isInfoEnabled()) LOG.info("Establishing access control for productId="+productId);
		
		// check authorization by using cookie or Json Web Token
		boolean authorized = checkCookie(req, productId) || checkJwt(req, productId);
				
		if (authorized) {
			  // request is authorized, keep processing
			  chain.doFilter(req, resp);
		} else {
			// authorization cookie was NOT found, or signature validation failed
			if (LOG.isDebugEnabled()) LOG.debug("Authorization failed for productID="+productId);
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sorry, you are not authorized to download this product.");
		}

	}
	
	/**
	 * Attempts authorization by checking a signed cookie containing the product id
	 * @return
	 */
	private boolean checkCookie(HttpServletRequest req, String productId) throws UnsupportedEncodingException {
				
		// retrieve cookie to check authorization
		boolean authorized = false;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
		      for (int i = 0; i < cookies.length; i++) {
		          Cookie cookie=cookies[i];
		          if (LOG.isDebugEnabled()) LOG.debug("Found cookie="+cookie.getName()+" value="+cookie.getValue());
		          if (cookie.getName().equals(Constants.COOKIE_PRODUCT_ID_NAME)) {
		        	  
		      		  // NOTE: the front-end URL-encodes the value before storing it in the cookie
		      		  // so the back-end must URL-decode it before validating the signature
			        	  String signature = URLDecoder.decode( cookie.getValue(), "UTF-8" );
			        	  if (LOG.isInfoEnabled()) LOG.info("Found authorization cookie: name="+cookie.getName()+" URL-decoded value="+signature);
		        	  		        		  
		        		  // validate signature
		        	      
		        		  if (rsaUtils.verify(productId, signature)) {
		        			  if (LOG.isInfoEnabled()) LOG.info("Cookie signature is valid");
		        		      authorized = true;
		        		  } else {
		        		      if (LOG.isWarnEnabled()) LOG.warn("Cookie signature is NOT valid");
		        		  }
		        		  
		          }
		       }
		 }
		
		return authorized;
	}
	
	/**
	 * Attempts authorization by using a Json Web Token that contains the productId in the 'sub' field
	 * @param req
	 * @param productId
	 * @return
	 */
	private boolean checkJwt(HttpServletRequest req, String productId) {
		
		boolean authorized = false;
				
		String authzHeader = req.getHeader("Authorization");
		if (authzHeader!=null && authzHeader.indexOf("Bearer")>=0) {
			
			try {
				String token = authzHeader.replaceFirst("Bearer", "").trim();
				LOG.info("Retrieved JWT="+token);
				
				DecodedJWT jwt = verifier.verify(token);
				String pId = jwt.getSubject();
				LOG.info("Retrieved product id = "+pId);
				if (pId.equals(productId)) {
					authorized = true;
				}
				
			} catch (JWTVerificationException e) {
				e.printStackTrace();
			}
			
		}
		
		return authorized;
		
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		// read private key location from filter configuration
		this.filterConfig = filterConfig;
		String privateKeyFilePath = this.filterConfig.getInitParameter("privateKeyFilePath");
		
		// replace env variable
		privateKeyFilePath = privateKeyFilePath.replace("[HOME]", System.getProperty("user.home"));
		if (LOG.isInfoEnabled()) LOG.info("Using private key file: "+privateKeyFilePath);
		
		// create re-usable signing utility
		rsaUtils = new RsaUtils(privateKeyFilePath);
		
		// object used to validate JWT
		verifier = JWT.require(Constants.algorithm)
				.withIssuer(Constants.ISSUER)
				.withAudience(Constants.AUDIENCE)
				.build();
		
	}

}
