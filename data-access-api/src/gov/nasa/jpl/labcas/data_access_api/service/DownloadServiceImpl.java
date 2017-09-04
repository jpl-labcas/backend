package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import gov.nasa.jpl.labcas.data_access_api.model.DownloadUrl;

@Path("/")
@Produces(MediaType.TEXT_PLAIN) 
public class DownloadServiceImpl implements DownloadService {

	@Override
	@GET
	@Path("/files/select")
	public Response downloadFiles(@Context HttpServletRequest httpRequest) {
		
		String result = "";
		
		System.out.println("HTTP Request="+httpRequest.getRequestURL().toString());
		
		List<DownloadUrl> urls = new ArrayList<DownloadUrl>();
		urls.add( new DownloadUrl("http://www.cnn.com/") );
		
		result = "http://www.cnn.com/" + "\n";
		result += "http://espn.com/" + "\n";
		//return urls.toArray(new DownloadUrl[urls.size()]);
		return Response.status(200).entity(result).build();
				
	}

}
