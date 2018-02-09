package gov.nasa.jpl.edrn.labcas.actions;

import static org.apache.oodt.cas.metadata.util.PathUtils.doDynamicReplacement;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.oodt.cas.crawl.action.CrawlerAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.commons.exec.ExecUtils;

import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

/**
 * Crawler action that executes external commands, possibly on a remote host via ssh.
 * Based on org.apache.oodt.cas.crawl.action.ExternAction but modified to enable remote ssh execution.
 * 
 * @author Luca Cinquini
 *
 */
public class SshExternAction extends CrawlerAction {

	protected String executeCommand;
	protected String workingDir;

	// compatible file extensions
	protected String extensions = "";

	protected Set<String> extensionsSet = new HashSet<String>();

	// parameters for remote ssh execution
	protected String sshHost = null;
	protected String sshUser = null;

	private final static String NEWLINE = System.getProperty("line.separator");

	/**
	 * File ~/labcas.properties
	 */
	protected Properties properties = new Properties();

	public SshExternAction() {
		super();
	}

	/**
	 * Executes the external command if the file matches one of the designated extensions.
	 * 
	 * @param product
	 * @param productMetadata
	 * @return
	 * @throws CrawlerActionException
	 */
	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {

		// determine file extension
		String extension = GeneralUtils.getFileExtension(product).toLowerCase();

		// process compatible extensions
		if (this.extensionsSet.contains(extension)) {

			// execute command
			try {
				String envReplacedExecuteCommand = doDynamicReplacement(executeCommand, productMetadata);
				LOG.info("Executing command: "+envReplacedExecuteCommand);
				return ExecUtils.callProgram(envReplacedExecuteCommand, LOG,
						new File(workingDir != null ? doDynamicReplacement(workingDir, productMetadata)
								: product.getParent())) == 0;
			} catch (Exception e) {
				LOG.warning("Failed to execute extern command '" + executeCommand + "' : " + e.getMessage());
				return false;
			}

		} else {

			// success if file extension does not match
			return true;
			
		}

	}

	/**
	 * Converts the 'extensions' String into a Set. 
	 * Also retrieves the optional parameters for ssh execution.
	 */
	@Override
	public void validate() throws CrawlerActionException {

		String[] extensionsArray = extensions.split(",");
		for (String ext : extensionsArray) {
			extensionsSet.add(ext.toLowerCase());
		}
		LOG.info("OHIF will process these file extensions: " + extensionsSet);

		// read ssh parameters from labcas.properties
		this.sshHost = properties.getProperty("sshHost");
		LOG.info("Using sshHost=" + sshHost);
		this.sshUser = properties.getProperty("sshUser");
		LOG.info("Using sshUser=" + sshUser);

		// wrap command through remote SSH login
		if (sshHost != null) {
			this.executeCommand = "ssh " + sshUser + "@" + sshHost + " <<EOF" + NEWLINE
		                        + this.executeCommand; 
		                        //+ NEWLINE + "EOF"; // note: this line is not parsed correctly, better remove it
		}

	}
	
	public void setExecuteCommand(String executeCommand) {
	      this.executeCommand = executeCommand;
	   }

	   public void setWorkingDir(String workingDir) {
	      this.workingDir = workingDir;
	   }

	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
