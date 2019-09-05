package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

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
			    		
		FileInputStream is = new FileInputStream(filePath.toFile());
		IOUtils.copyLarge(is, os);
		os.flush();
	    
	}	
	
}