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
 * the DatasetId must be stored in a thread-local map keyed to the specific product type name.
 * 
 * @author cinquini
 *
 */
public class LabcasProductIdGenerator implements ProductIdGenerator {
	
	// dataset information is stored at thread-local scope 
	// and populated by metadata extractor client the first time it is called
	// Map: ProductTypeName --> latest DatasetId to publish
	private static final ThreadLocal<Map<String,String>> datasetIdStore = new ThreadLocal<Map<String,String>>();
	
	private static final Logger LOG = Logger.getLogger(LabcasProductIdGenerator.class.getName());
	
	/**
	 * Static constructor initializes the specific thread-local store object.
	 */
	static {
		datasetIdStore.set( new HashMap<String, String>() );
		LOG.info("SET THE MAP");
	}
		
	public String generateId(Product product) {
		
		ProductType pt = product.getProductType();
		String datasetId = null; // use 'null' for mark file LABCAS_DATASET_INFO_FILE
		if (!product.getProductName().equals(Constants.LABCAS_DATASET_INFO_FILE)) {
			Map<String, String> dmap = datasetIdStore.get();
			if (dmap.containsKey(pt.getName())) {
				datasetId = dmap.get(pt.getName());
			} else {
				throw new RuntimeException("Cannot retrieve DatasetId for product type name="+pt.getName()
				                          +", product name="+product.getProductName());
			}
		} 
		LOG.fine("Product type="+pt.getName()+", using DatasetId="+datasetId
				+" from thread local scope from thread="+Thread.currentThread().getId());
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
	 * Method to store a datasetId at thread local scope.
	 * @param datasetId
	 */
	public static void setDatasetId(final String productTypeName, final String datasetId) {
		Map<String, String> dmap = datasetIdStore.get();
		LOG.info("Map is equal to:"+ dmap);
		if (dmap==null) {
			datasetIdStore.set( new HashMap<String, String>() );
		}
		datasetIdStore.get().put(productTypeName, datasetId);
	}

}