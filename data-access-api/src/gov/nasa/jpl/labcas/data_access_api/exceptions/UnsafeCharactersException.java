package gov.nasa.jpl.labcas.data_access_api.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

/**
 * Custom exception thrown when unsafe characters are detected in the HTTP request.
 * 
 * @author Luca Cinquini
 *
 */
public class UnsafeCharactersException extends WebApplicationException {
	
	private static final long serialVersionUID = -159557983635549537L;

	public UnsafeCharactersException(String message, Status status) {
		super(message, status);
	}

}
