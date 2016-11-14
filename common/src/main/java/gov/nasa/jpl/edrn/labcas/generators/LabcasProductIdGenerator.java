package gov.nasa.jpl.edrn.labcas.generators;

import org.apache.oodt.cas.filemgr.catalog.solr.ProductIdGenerator;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.structs.ProductType;

/**
 * Class that generates a product id of the form:
 * "<product type name>.<product name>"
 * which is the only information available for each product before all detailed metadata is pushed to the server.
 * 
 * Note that neither the dataset id nor version are available to create the product id, which is therefore not necessarily unique.
 * As a consequence, newer records may override older records in the Solr 'oodt-fm' core.
 * 
 * @author cinquini
 *
 */
public class LabcasProductIdGenerator implements ProductIdGenerator {
			
	public String generateId(Product product) {
		
		ProductType pt = product.getProductType();
		return pt.getName()+"."+product.getProductName();
				
	}
	
}