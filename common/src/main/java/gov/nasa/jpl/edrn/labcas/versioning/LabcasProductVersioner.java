package gov.nasa.jpl.edrn.labcas.versioning;

import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.exceptions.VersioningException;
import org.apache.oodt.cas.filemgr.versioning.MetadataBasedFileVersioner;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * Class that defines the directory structure used to archive LabCAS products.
 * Inspired by "gov.nasa.jpl.edrn.ecas.versioning.EDRNProductVersioner".
 * 
 * @author Luca Cinquini
 */
public class LabcasProductVersioner extends MetadataBasedFileVersioner {
	
	//private String filePathSpec = "/[SiteShortName]/[ProductReceivedDate]/[OrganName]/[InstrumentId]/[Filename]";
	private String filePathSpec = "/[DatasetId]/[Version]/[Filename]";
	
    public LabcasProductVersioner() {
        setFlatProducts(true);
        setFilePathSpec(filePathSpec);
    }
    
    @Override
    public void createDataStoreReferences(Product product, Metadata metadata) throws VersioningException {
    	
    	super.createDataStoreReferences(product, metadata);
    	
    }

}
