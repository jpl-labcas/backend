package gov.nasa.jpl.labcas.data_access_api.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

/**
 * Custom exception that denotes a missing Basic authentication header.
 * This exception is caught by the custom @see MissingAuthenticationHeaderExceptionHandler 
 * to trigger the Basic authentication dialog presented by the client.
 * 
 * @author Luca Cinquini
 *
 */
public class MissingAuthenticationHeaderException extends WebApplicationException {
	
	private static final long serialVersionUID = -159557983635549537L;

	public MissingAuthenticationHeaderException(Status status) {
		super(status);
	}

}
