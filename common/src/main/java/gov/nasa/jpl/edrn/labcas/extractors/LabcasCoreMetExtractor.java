package gov.nasa.jpl.edrn.labcas.extractors;

import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.exceptions.MetExtractionException;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.generators.LabcasProductIdGenerator;

/**
 * Sub-class of @see {@link CoreMetExtractor} that stores the DatasetId information at thead-local scope
 * so it can be used for generating the id of all following products.
 * @author cinquini
 *
 */
public class LabcasCoreMetExtractor extends CoreMetExtractor {
	
	private static final Logger LOG = Logger.getLogger(LabcasCoreMetExtractor.class.getName());
	
	@Override
	public Metadata doExtract(Product product, Metadata met) throws MetExtractionException {
		
		Metadata _met = super.doExtract(product, met);
		
		String datasetId = _met.getMetadata(Constants.METADATA_KEY_DATASET_ID);
		String productTypeName = _met.getMetadata(Constants.METADATA_KEY_PRODUCT_TYPE);
		if (productTypeName!=null && datasetId != null) {
			
			LOG.fine("Storing ProductType="+productTypeName+" --> DatasetId="+datasetId+" at static scope for later use");
			LabcasProductIdGenerator.setDatasetId(productTypeName, datasetId);
			
		}
				
		
		return _met;
		
	}

}
