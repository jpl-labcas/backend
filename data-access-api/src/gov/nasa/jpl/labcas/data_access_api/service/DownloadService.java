package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface DownloadService {
	
	public Response downloadFiles(HttpServletRequest httpRequest, String q, List<String> fq, int start, int rows, String sort);

}
