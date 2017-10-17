package gov.nasa.jpl.labcas.data_access_api.exceptions;

import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Class that catches the custom @see MissingAuthenticationHeaderException
 * and sends the "WWW-Authenticate" header to the client, 
 * so it can present a username/password dialog to the user.
 * 
 * @author Luca Cinquini
 *
 */
@Provider
public class MissingAuthenticationHeaderExceptionHandler implements ExceptionMapper<MissingAuthenticationHeaderException> {
	
	
	private final static String REALM = "LabCAS";
	private final static Logger LOG = Logger.getLogger(MissingAuthenticationHeaderExceptionHandler.class.getName());
	
    @Override
    public Response toResponse(MissingAuthenticationHeaderException  err) {
        
        LOG.fine("Handling Missing Authentication Header Exception :" + err);
        return Response.status(Status.UNAUTHORIZED)
        		       .entity("Missing BASIC Authentication Header")
        		       .header("WWW-Authenticate", "Basic realm=\""+REALM+"\"").build();   
    }
	

}
