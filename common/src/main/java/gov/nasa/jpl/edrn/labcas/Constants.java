package gov.nasa.jpl.edrn.labcas;

/**
 * Interface containing constants for Labcas-backend module.
 * 
 * @author cinquini
 *
 */
public interface Constants {
	
	// envaronment variables
	public final static String ENV_LABCAS_STAGING = "LABCAS_STAGING";
	public final static String ENV_LABCAS_ARCHIVE = "LABCAS_ARCHIVE";
	
	// workflows
	public final static String WORKFLOW_LABCAS_UPOLOAD = "labcas-upload";
	
	// metadata keys
	public final static String METADATA_KEY_DATASET = "Dataset";
	public final static String METADATA_KEY_VERSION = "Version";
	
	// dataset-level metadata file
	public final static String METADATA_FILE = "DatasetMetadata.xml";
	
	// OODT metadata extension
	public final static String METADATA_EXTENSION = ".met";
	
	// maximum number of records returned by Solr
	// (i.e. maximum number of files in a single dataset that can be updated)
	public final static int MAX_SOLR_ROWS = 1000;
	
}