package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

/**
 * Class that downloads a file by streaming its contents.
 *
 */
public class DownloadHelper implements StreamingOutput {
	
	private Path filePath;
	
	public DownloadHelper(Path filePath) {
		this.filePath = filePath;
	}

	@Override
	public void write(OutputStream os) throws IOException, WebApplicationException {
			    
	    byte[] data = Files.readAllBytes(filePath);
        os.write(data);
        os.flush();
	    
	}	
	
}