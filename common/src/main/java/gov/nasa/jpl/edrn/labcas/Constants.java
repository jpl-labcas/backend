package gov.nasa.jpl.edrn.labcas;

/**
 * Interface containing constants for Labcas-backend module.
 * 
 * @author cinquini
 *
 */
public interface Constants {
	
	// environment variables
	public final static String ENV_LABCAS_HOME = "LABCAS_HOME";
	public final static String ENV_LABCAS_STAGING = "LABCAS_STAGING";
	public final static String ENV_LABCAS_ARCHIVE = "LABCAS_ARCHIVE";
	
	public final static String ENV_FILEMGR_URL = "FILEMGR_URL";
	public final static String ENV_WORKFLOW_URL = "WORKFLOW_URL";
	public final static String ENV_SOLR_URL = "SOLR_URL";
	
	// workflows
	public final static String WORKFLOW_LABCAS_UPOLOAD = "labcas-upload";
	
	// metadata keys
	public final static String METADATA_KEY_DATASET_ID = "DatasetId";
	public final static String METADATA_KEY_NEW_VERSION = "NewVersion";
	public final static String METADATA_KEY_VERSION = "Version";
	public final static String METADATA_KEY_DESCRIPTION = "Description";
	public final static String METADATA_KEY_FILE_LOCATION = "FileLocation";
	public final static String METADATA_KEY_FILE_NAME = "Filename";
	public final static String METADATA_KEY_PRODUCT_NAME = "ProductName";
	
	// dataset-level metadata file
	public final static String METADATA_FILE = "DatasetMetadata.xmlmet";
	
	// policy directory
	public final static String POLICY = "policy";
	
	// metadata file extensions
	public final static String OODT_METADATA_EXTENSION = ".met";
	public final static String EDRN_METADATA_EXTENSION = ".xmlmet";
	
	// maximum number of records returned by Solr
	// (i.e. maximum number of files in a single dataset that can be updated)
	public final static int MAX_SOLR_ROWS = 1000;
	
	// XML parameters
	public final static String PREFIX = "cas";
	public final static String NS = "http://oodt.apache.org/components/cas";
	
	// FIXME: will be replaced with same values in CoreMetKeys.java
	public final static String PRODUCT_TYPE = "ProductType";

	
}