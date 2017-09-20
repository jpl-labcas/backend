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
		
	// metadata keys
	public final static String METADATA_KEY_COLLECTION_NAME = "CollectionName";
	public final static String METADATA_KEY_COLLECTION_ID = "CollectionId";
	public final static String METADATA_KEY_COLLECTION_DESCRIPTION = "CollectionDescription";
	
	public final static String METADATA_KEY_DATASET_NAME = "DatasetName";
	public final static String METADATA_KEY_DATASET_ID = "DatasetId";
	public final static String METADATA_KEY_DATASET_DESCRIPTION = "DatasatDescription";
	public final static String METADATA_KEY_DATASET_VERSION = "DatasetVersion";
	
	public final static String METADATA_KEY_PRODUCT_TYPE_NAME = "ProductTypeName";
	public final static String METADATA_KEY_PRODUCT_NAME = "ProductName";
	public final static String METADATA_KEY_PRODUCT_TYPE = "ProductType";
	
	public final static String METADATA_KEY_OWNER_PRINCIPAL = "OwnerPrincipal";
	
	// separates multiple metadata values for the same key
	public final static String METADATA_KEY_SEPARATOR = "|";
		
	// flags
	public final static String METADATA_KEY_NEW_VERSION = "NewVersion"; // default: false
	public final static String METADATA_KEY_UPDATE_COLLECTION = "UpdateCollection"; // default: true
	public final static String METADATA_KEY_UPLOAD_FILES = "UploadFiles";
	public final static String METADATA_KEY_WORKFLOW_ORDER = "workflow.order";
	public final static String METADATA_KEY_NOQUIP = "NOQUIP";  // if set, skip publishing file to QUIP viewer
	
	
	public final static String METADATA_KEY_FILE_LOCATION = "FileLocation"; // points to original location
	public final static String METADATA_KEY_FILE_PATH = "FilePath";          // points to archive location
	public final static String METADATA_KEY_FILE_NAME = "Filename";
	public final static String METADATA_KEY_FILE_TYPE = "FileType";
	public final static String METADATA_KEY_FILE_SIZE = "FileSize";
		
	
	public final static String METADATA_KEY_ID = "id";
	public final static String METADATA_KEY_NAME = "Name";
	
	//public final static String METADATA_KEY_DESCRIPTION = "Description";
	public final static String METADATA_KEY_FILE_DOWNLOAD_ID = "FileDownloadId";
	public final static String METADATA_KEY_TIMESTAMP = "timestamp";

	
	// files containing optional additional metadata
	public final static String DATASET_METADATA_FILE = "DatasetMetadata.xmlmet";
	public final static String COLLECTION_METADATA_FILE = "CollectionMetadata.xmlmet";
	
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
	
	public final static String EDRN_PREFIX = "urn:edrn:";
	public final static String LABCAS_PRODUCT_TYPE = "LabcasProduct";
	public final static String ECAS_PRODUCT_TYPE = "EcasProduct";
	
	// external URL types
	public final static String URL_TYPE_CAMICROSCOPE = "caMicroscope Viewer";
	
	// HTTP REQUEST PARAMETERS
	public final static String PARAMETER_PRODUCT_ID = "productID";
	
	// HTTP COOKIES
	//public final static String COOKIE_PRODUCT_ID_NAME = "labcasProductIDcookie";
	public final static int COOKIE_PRODUCT_ID_LIFETIME = 3600; // one hour
	public final static String COOKIE_PRODUCT_ID_NAME = "labcasProductIDcookie";
	public final static String COOKIE_PRODUCT_ID_NAME2 = "labcasProductIDcookie2";
	
}