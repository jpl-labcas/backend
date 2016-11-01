package gov.nasa.jpl.edrn.labcas.actions;

import java.io.File;
import java.net.URL;

import org.apache.oodt.cas.crawl.action.CrawlerAction;
import org.apache.oodt.cas.crawl.structs.exceptions.CrawlerActionException;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.generators.LabcasProductIdGenerator;
import gov.nasa.jpl.edrn.labcas.utils.SolrUtils;

/**
 * Class that is executed after a product (aka file) is succesfully ingested into OODT. Specifically:
 * - the marker file '000_labcas_info.txt' is immediately unpublished from Solr core 'oodt-fm'
 * - all other files are published to the Solor core 'files'
 * 
 * @author cinquini
 *
 */
public class LabcasPostIngestionAction extends CrawlerAction {
	
    // URL of File Manager instance with default value
    private String fileManagerUrl = "http://localhost:9000/";

	@Override
	public boolean performAction(File product, Metadata productMetadata) throws CrawlerActionException {
		
		LOG.info("Performing post-ingest-on-success action for file: "+product.getAbsolutePath());
		
		// unpublish marker file
		String productName = productMetadata.getMetadata(Constants.METADATA_KEY_PRODUCT_NAME);
		if (productName.equals(Constants.LABCAS_DATASET_INFO_FILE)) {
			
			String productId = LabcasProductIdGenerator.generateId(productMetadata.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE), 
																   null, // NOTE: must use DatasetId=null
					                                               //productMetadata.getMetadata(Constants.METADATA_KEY_DATASET_ID),
					                                               productMetadata.getMetadata(Constants.METADATA_KEY_PRODUCT_NAME) );
			
			try {
				LOG.fine("Unpublishing product with id="+productId+" from File Manager at:"+this.fileManagerUrl);
				XmlRpcFileManagerClient client = new XmlRpcFileManagerClient(new URL(fileManagerUrl));
				
				Product p = new Product();
				p.setProductId(productId);
				client.removeProduct(p);
				
			} catch(Exception e) {
				throw new CrawlerActionException( e.getMessage() );
			}
			
		// publish product into Solr files core
		} else {
			
			// publish product
			try {
				SolrUtils.publishProduct(productMetadata);
			} catch (Exception e) {
				throw new CrawlerActionException( e.getMessage() );
			}
			
		}
		
		// return success status
		return true;
	}
	
	@Override
	public void validate() throws CrawlerActionException {}
	
    public void setFileManagerUrl(String fileManagerUrl) {
        this.fileManagerUrl = fileManagerUrl;
    }

}
