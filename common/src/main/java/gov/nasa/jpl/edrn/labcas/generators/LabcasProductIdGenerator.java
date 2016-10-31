package gov.nasa.jpl.edrn.labcas.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.catalog.solr.ProductIdGenerator;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Class that generates a product id of the form:
 * "<product type name>.<dataset id>.<dataset version>.<product name>"
 * which guarantees a unique identifier for a product type, dataset, version and file.
 * 
 * Because the generateId() signature does not allow access to the product metadata,
 * the DatasetId must be stored in a static map keyed to the specific product type name.
 * 
 * @author cinquini
 *
 */
public class LabcasProductIdGenerator implements ProductIdGenerator {
	
	// dataset information is stored at static scope 
	// and populated by the metadata extractor client the first time it is called
	// Map: ProductTypeName --> latest DatasetId to publish
	private static final Map<String,String> datasetIdStore = new HashMap<String, String>();
	
	private static final Logger LOG = Logger.getLogger(LabcasProductIdGenerator.class.getName());
		
	public String generateId(Product product) {
		
		ProductType pt = product.getProductType();
		String datasetId = null; // use 'null' for mark file LABCAS_DATASET_INFO_FILE
		if (!product.getProductName().equals(Constants.LABCAS_DATASET_INFO_FILE)) {
			if (datasetIdStore.containsKey(pt.getName())) {
				datasetId = datasetIdStore.get(pt.getName());
			} else {
				throw new RuntimeException("Cannot retrieve DatasetId for product type name="+pt.getName()
				                          +", product name="+product.getProductName());
			}
		} 
		LOG.fine("Product type="+pt.getName()+", using DatasetId="+datasetId);
		Metadata ptm = pt.getTypeMetadata();
		String version = ptm.getMetadata(Constants.METADATA_KEY_DATASET_VERSION);
				
		String id = null;
		if (version!=null) {
			// example: "mydata.dataset-id.1.file1.txt"
			id = pt.getName()+"."+datasetId+"."+version+"."+product.getProductName();
		} else {
			// example: "mydata.dataset-id.file1.txt"
			id = pt.getName()+"."+datasetId+"."+product.getProductName();
		}
		
		return id;
		
	}
	
	/**
	 * Method to store the next DatasetId into the static map.
	 * @param datasetId
	 */
	public static synchronized void setDatasetId(final String productTypeName, final String datasetId) {
		datasetIdStore.put(productTypeName, datasetId);
	}

}