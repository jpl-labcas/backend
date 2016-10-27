package gov.nasa.jpl.edrn.labcas.versioning;

import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.exceptions.VersioningException;
import org.apache.oodt.cas.filemgr.versioning.MetadataBasedFileVersioner;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * Class that defines the directory structure used to archive NIST workflow products.
 * Metadata fields available for substitution are retrieved from the dynamic workflow metadata.
 * 
 * @author Luca Cinquini
 */
public class NistProductVersioner extends MetadataBasedFileVersioner {
	
	//private String filePathSpec = "/[LabNumber]/[Method]/[RoundNumber]";
	private String filePathSpec = "/[DatasetId]/";
	
    public NistProductVersioner() {
        setFlatProducts(true);
        setFilePathSpec(filePathSpec);
    }
    
    @Override
    public void createDataStoreReferences(Product product, Metadata metadata) throws VersioningException {
    	
    	super.createDataStoreReferences(product, metadata);
    	
    }

}
