package gov.nasa.jpl.labcas.data_access_api.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Utility class to parse a LabCAS XML document 
 * into the required parameters needed to invoke the MetadataService
 * 	
 	<updates action="set">
   		<update>
      		<query>id:test.test.v1.testData.nc|esgf-dev.jpl.nasa.gov</query>
      		<field name="xlink">
         		<value>abc</value>
         		<value>123</value>
      		</field>
   		</update>
	</updates>
 * 
 * @author Luca Cinquini
 *
 */
public class UpdateDocumentParser {
	
	private String action;
	private String core;
	private HashMap<String, Map<String,List<String>>> doc = new HashMap<String, Map<String,List<String>>>();
	
	public UpdateDocumentParser(String xmlString) throws Exception {
		
		// parse XML
		XmlParser xmlParser = new XmlParser(false);
		Document xmlDoc = xmlParser.parseString(xmlString);
		Element root = xmlDoc.getRootElement();
		
		// Solr core
		this.core = root.getAttributeValue("core");
		
		// action=set/add/remove
		this.action = root.getAttributeValue("action");
		
		// loop over separate updates
		for (Object updateObj : root.getChildren("update")) {
			
			
			Element updateEl = (Element)updateObj;
			Element queryEl = (Element)updateEl.getChild("query");
			String query = queryEl.getTextTrim();
	
			Map<String,List<String>> metadata = new HashMap<String,List<String>>();
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
	
	public String getCore() {
		return core;
	}

	public HashMap<String, Map<String,List<String>>> getDoc() {
		return doc;
	}
	
}
