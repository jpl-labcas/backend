package gov.nasa.jpl.edrn.labcas.generators;

import org.apache.oodt.cas.filemgr.catalog.solr.ProductIdGenerator;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.ProductType;
import org.apache.oodt.cas.metadata.Metadata;

import gov.nasa.jpl.edrn.labcas.Constants;

/**
 * Class that generates a product id of the form:
 * "<product type name>.<product_type_version>.<product name>"
 * which guarantees a unique identfier for a given file, dataset and version.
 * 
 * @author cinquini
 *
 */
public class ProductTypeDotVersionDotProductIdGenerator implements ProductIdGenerator {
		
	public String generateId(Product product) {
		
		ProductType pt = product.getProductType();
		Metadata ptm = pt.getTypeMetadata();
		String version = ptm.getMetadata(Constants.METADATA_KEY_DATASET_VERSION);
				
		String id = null;
		if (version!=null) {
			// example: "mydata.1.file1.txt"
			id = pt.getName()+"."+version+"."+product.getProductName();
		} else {
			// example: "mydata.file1.txt"
			id = pt.getName()+"."+product.getProductName();
		}
		
		return id;
		
	}

}