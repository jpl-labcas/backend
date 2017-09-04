package gov.nasa.jpl.labcas.data_access_api.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface DownloadService {
	
	public Response downloadFiles(HttpServletRequest httpRequest);

}
