import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.nasa.jpl.edrn.labcas.Constants;
import gov.nasa.jpl.edrn.labcas.Utils;

public class MyTest {
	
	private static final Logger LOG = Logger.getLogger(MyTest.class.getName());

	public static void main(String[] args) throws Exception {
		
		String datasetName = "mydata"; // FIXME: metadata.getMetadata(Constants.METADATA_KEY_DATASET)
		
		// determine latest dataset version
		int version = Utils.findLatestDatasetVersion( datasetName );
		LOG.info("VERSION="+version);
		
		
        // retrieve all documents matching the given dataset name and version
        String url = "http://edrn-frontend.jpl.nasa.gov:8080/solr/oodt-fm";
        SolrServer server = new CommonsHttpSolrServer(url);
        
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*");
        query.addFilterQuery("Dataset:"+datasetName,"Version:"+version);
        
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
		//String xml = MyTest.readDatasetXml( datasetName );
        //LOG.info("XML="+xml);
        
        // update Solr document
        //MyTest.updateDocuments(xml);
                
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
	
	
	private static String readDatasetXml(String datasetName) throws Exception {
				
		// read input metadata
        Metadata datasetMetadata = Utils.readDatasetMetadata(datasetName);
        
        // create Solr/XML update document
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document xmlDocument = builder.newDocument();
        
        // <add>
        Element addElement = xmlDocument.createElement("add");
        xmlDocument.appendChild(addElement);
        
        // <doc> (for each file)
        Element docElement = xmlDocument.createElement("doc");
        addElement.appendChild(docElement);
        
        // <field name="id">38b6e7e6-3a9b-4565-9d57-37e8104b4fde</field>
        Element fieldElement = xmlDocument.createElement("field");
        fieldElement.setAttribute("name", "id");
        fieldElement.insertBefore(xmlDocument.createTextNode("38b6e7e6-3a9b-4565-9d57-37e8104b4fde"), fieldElement.getLastChild());
        docElement.appendChild(fieldElement);
        
        // <field name="Institution" update="set">Darthmouth</field>
		 for (String key : datasetMetadata.getAllKeys()) {
			for (String val : datasetMetadata.getAllMetadata(key)) {
				LOG.info("\t==> XML: Updating dataset metadata key=["+key+"] value=["+val+"]");
				
				Element metFieldElement = xmlDocument.createElement("field");
				metFieldElement.setAttribute("name", key);
				metFieldElement.setAttribute("update", "set");
				metFieldElement.insertBefore(xmlDocument.createTextNode(val), metFieldElement.getLastChild());
		        docElement.appendChild(metFieldElement);
				
			}
		 }

        String xmlString = MyTest.prettyPrint(xmlDocument);
        LOG.info(xmlString);
        return xmlString;

		
	}
	
	 public static final String prettyPrint(Document xml) throws Exception {
		 Transformer tf = TransformerFactory.newInstance().newTransformer();
		 tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		 tf.setOutputProperty(OutputKeys.INDENT, "yes");
		 Writer out = new StringWriter();
		 tf.transform(new DOMSource(xml), new StreamResult(out));
		 return out.toString();
	 }


}
