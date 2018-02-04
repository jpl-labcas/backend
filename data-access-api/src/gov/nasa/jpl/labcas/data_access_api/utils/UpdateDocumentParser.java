package gov.nasa.jpl.labcas.data_access_api.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.jdom.Document;
import org.jdom.Element;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class to parse a LabCAS XML or JSON document 
 * into the required parameters needed to invoke the MetadataService.
 * 
 * Example XML document:
	<updates core="collections" action="set">
	   <update>
	      <query>id:Boston_University_Lung_Tumor_Sequencing</query>
	      <field name="LeadPI">
	         <value>John Smith</value>
	         <value>Jane Johns</value>
	      </field>
	   </update>
	</updates>
 * 
 * Example JSON document:
	{  
	   "core": "collections",
	   "action": "set",
	   "updates": [
	      {  
	         "query": "id:Boston_University_Lung_Tumor_Sequencing",
	         "fields": [
	            {
	               "name": "LeadPI",
	               "values": [
	                  "John Smith",
	                  "Jane Johns"
	               ]  
	            }  
	         ]  
	      }  
	   ]  
	}
 * 
 * @author Luca Cinquini
 *
 */
public class UpdateDocumentParser {
	
	private String action;
	private String[] cores;
	private HashMap<String, Map<String,List<String>>> doc = new HashMap<String, Map<String,List<String>>>();
	
	private final static Logger LOG = Logger.getLogger(UpdateDocumentParser.class.getName());
	
	public UpdateDocumentParser(final String docString, final String contentType) throws Exception {
		
		// "application/xml" or "text/xml"
		if (contentType.equals(MediaType.APPLICATION_XML) || contentType.equals(MediaType.TEXT_XML)) {
			parseXml(docString);
			
		// "application/json"
		} else if (contentType.equals(MediaType.APPLICATION_JSON)) {
			parseJson(docString);
			
		} else {
			throw new Exception("Unknown document content type.");			
		}
		
	}
	
	private void parseJson(String docString) throws Exception {
		
		// parse JSON
		JSONObject jsonDoc = new JSONObject(docString);
		LOG.info(jsonDoc.toString());
		
		// Solr cores
		this.cores = jsonDoc.getString("core").split(",");
		
		// action=set/add/remove
		this.action = jsonDoc.getString("action");
		
		// loop over separate updates
		JSONArray updates = (JSONArray)jsonDoc.get("updates");
		for (int i=0; i< updates.length(); i++) {
		    JSONObject update = updates.getJSONObject(i);
		    
		    String query = update.getString("query").trim();
		    Map<String,List<String>> metadata = new HashMap<String,List<String>>();
		    
		    // loop over fields
		    JSONArray fields = (JSONArray)update.get("fields");
		    for (int j=0; j<fields.length(); j++) {
		    		JSONObject field = fields.getJSONObject(j);
		    	
		    		String fieldName = field.getString("name");
		    		List<String> fieldValues = new ArrayList<String>();
		    		
		    		if (field.has("values")) {
			    		JSONArray values = field.getJSONArray("values");
			    		for (int k=0; k<values.length(); k++) {
			    			String value = values.getString(k);
			    			fieldValues.add(value.trim());
			    		}
		    		}
		    		
		    		metadata.put(fieldName, fieldValues);
		    	
		    }
		    
		    this.doc.put(query, metadata);
		    
		}

		
	}
	
	private void parseXml(String docString) throws Exception {
		
		// parse XML
		XmlParser xmlParser = new XmlParser(false);
		Document xmlDoc = xmlParser.parseString(docString);
		Element root = xmlDoc.getRootElement();
		
		// Solr cores
		this.cores = root.getAttributeValue("core").split(",");
		
		// action=set/add/remove
		this.action = root.getAttributeValue("action");
		
		// loop over separate updates
		for (Object updateObj : root.getChildren("update")) {
			
			
			Element updateEl = (Element)updateObj;
			Element queryEl = (Element)updateEl.getChild("query");
			String query = queryEl.getTextTrim();
	
			Map<String,List<String>> metadata = new HashMap<String,List<String>>();
			
			// loop over fields
			for (Object fieldObj : updateEl.getChildren("field")) {
				
				Element fieldEl = (Element)fieldObj;
				String fieldName = fieldEl.getAttributeValue("name");
				List<String> fieldValues = new ArrayList<String>();
				
				for (Object valueObj : fieldEl.getChildren("value")) {
					Element valueEl = (Element)valueObj;
					fieldValues.add(valueEl.getTextTrim());
				}
				
				metadata.put(fieldName, fieldValues);
				
			}
			
			this.doc.put(query, metadata);
			
		}
		
	}
	
	public String getAction() {
		return action;
	}
	
	public String[] getCores() {
		return cores;
	}

	public HashMap<String, Map<String,List<String>>> getDoc() {
		return doc;
	}
	
	public static void main(String[] args) throws Exception {
		
		String filepath = "/Users/cinquini/tmp/example_set1.json";
		byte[] encoded = Files.readAllBytes(Paths.get(filepath));
		String docString = new String(encoded, "UTF-8");
		
		UpdateDocumentParser self = new UpdateDocumentParser(docString, MediaType.APPLICATION_JSON);
		LOG.info(self.getAction());
		LOG.info(self.getCores().toString());
		LOG.info(self.getDoc().toString());
		
		
	}
	
}
