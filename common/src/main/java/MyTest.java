import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import gov.nasa.jpl.edrn.labcas.Constants;

public class MyTest {
	
	private static final Logger LOG = Logger.getLogger(MyTest.class.getName());

	public static void main(String[] args) throws Exception {
		
		String datasetName = "DatasetMetadata.xml"; // FIXME: metadata.getMetadata(Constants.METADATA_KEY_DATASET)
		
		// determine latest dataset version
		int version = MyTest.findLatestDatasetVersion( datasetName );
		LOG.info("VERSION="+version);
		
		
        // retrieve all documents matching the given dataset name and version
        String url = "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm";
        SolrServer server = new CommonsHttpSolrServer(url);
        
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        query.set("Dataset", "mydata");
        query.set("Version", ""+version);
        
        QueryResponse rsp = server.query( query );
        SolrDocumentList docs = rsp.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
        while (iter.hasNext()) {
            SolrDocument doc = iter.next();
            LOG.info(doc.toString());
            String id = (String) doc.getFieldValue("id"); 
            LOG.info("retrieved document id="+id);
        }
        
		// read updated metadata from DatasetMetadata.xml
		String xml = MyTest.readDatasetXml( datasetName );
        LOG.info("XML="+xml);
        
        // update Solr document
        MyTest.updateDocuments(xml);
                
	}
	
	private static void updateDocuments(String xmlUpdateDocument) throws Exception {
        String strURL = "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm/update?commit=true";

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(strURL);
        
        	
        HttpEntity entity = new ByteArrayEntity(xmlUpdateDocument.getBytes("UTF-8"));
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/xml");
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());

        // TODO: close connection
		
	}
	
	
	// utility function to determine the latest dataset version
	// if not found, the latest version is set to 0
	private static int findLatestDatasetVersion(String datasetName) {
		
		String archiveDir = System.getenv(Constants.ENV_LABCAS_ARCHIVE) + "/" + Constants.WORKFLOW_LABCAS_UPOLOAD;
		File datasetDir = new File(archiveDir, datasetName); 
		LOG.info("datasetDir="+datasetDir.getAbsolutePath());
		
        int version = 0;
        if (datasetDir.exists()) {           
	        LOG.fine("Looking for dataset versions in "+datasetDir.getAbsolutePath());
	
	        // list "version" sub-directories
	        String[] directories = datasetDir.list(new FilenameFilter() {
	                  @Override
	                  public boolean accept(File current, String name) {
	                    return new File(current, name).isDirectory();
	                  }
	                });
	        for (String dir : directories) {
	        	int v = Integer.parseInt(dir);
	        	if (v > version) version = v;
	        }    
        }
        
        return version;
		
	}
	
	private static String readDatasetXml(String datasetName) throws IOException {
		
        String stagingDir = System.getenv(Constants.ENV_LABCAS_STAGING) + "/" + datasetName;
        File datasetMetadataFile = new File(stagingDir, Constants.METADATA_FILE);
		
		// read input metadata
        if (datasetMetadataFile.exists()) {
        	LOG.info("Updating metadata from file: "+datasetMetadataFile.getAbsolutePath());
        	
        	try {
        		 SerializableMetadata sm = new SerializableMetadata("UTF-8", false);
        		 sm.loadMetadataFromXmlStream(datasetMetadataFile.toURI().toURL().openStream());
        		 Metadata datasetMetadata = sm.getMetadata();
     			 for (String key : datasetMetadata.getAllKeys()) {
    				for (String val : datasetMetadata.getAllMetadata(key)) {
    					LOG.info("\t==> Updating dataset metadata key=["+key+"] value=["+val+"]");
    				}
     			 }
        		 
        	} catch (Exception e) {
        		LOG.warning(e.getMessage());
        	}
        }
        
        // FIXME: must transform metadata into Solr XML update document
        String strXMLFilename = "/Users/cinquini/tmp/doc.xml";
        File input = new File(strXMLFilename);
        String xml = FileUtils.readFileToString(input);
        return xml;
		
	}

}
