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

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.utils.GeneralUtils;

/**
 * Crawler action that executes external commands, possibly on a remote host via ssh.
 * Based on org.apache.oodt.cas.crawl.action.ExternAction but modified to enable remote ssh execution.
 * 
 * @author Luca Cinquini
 *
 */
public class SshExternAction extends CrawlerAction {

	private String executeCommand;
	private String workingDir;

	// compatible file extensions
	private String extensions = "";

	private Set<String> extensionsSet = new HashSet<String>();

	// parameters for remote ssh execution
	private String sshHost = null;
	private String sshUser = null;

	private final static String NEWLINE = System.getProperty("line.separator");

	/**
	 * File ~/labcas.properties
	 */
	private Properties properties = new Properties();

	public SshExternAction() {
		super();
	}

	/**
	 * Executes the external command if the file matches one of the designated
	 * extensions.
	 * 
	 * @param product
	 * @param productMetadata
	 * @return
	 * @throws CrawlerActionException
	 */
	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {

		// only proceed if NOOHIF flag is not set
		if (!productMetadata.containsKey(Constants.METADATA_KEY_NOOHIF)) {

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

			}

		}

		// success if no action is taken
		return true;

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
			this.executeCommand = "ssh " + sshUser + "@" + sshHost + " \"sh -c '"
		                        + this.executeCommand 
		                        + "'\"";
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
