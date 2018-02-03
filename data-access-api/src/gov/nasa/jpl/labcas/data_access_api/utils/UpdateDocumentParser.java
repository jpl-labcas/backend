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
 * Example document to parse:
 * 
	<updates core="collections,datasets,files" action="set">
	   <update>
	      <query>id:Boston_University_Lung_Tumor_*</query>
	      <field name="OwnerPrincipal">
	         <value>uid=testuser,dc=edrn,dc=jpl,dc=nasa,dc=gov</value>
	         <value>cn=Spira Boston University,ou=groups,o=MCL</value>
	      </field>
	   </update>
	</updates>
 * 
 * @author Luca Cinquini
 *
 */
public class UpdateDocumentParser {
	
	private String action;
	private String[] cores;
	private HashMap<String, Map<String,List<String>>> doc = new HashMap<String, Map<String,List<String>>>();
	
	public UpdateDocumentParser(String xmlString) throws Exception {
		
		// parse XML
		XmlParser xmlParser = new XmlParser(false);
		Document xmlDoc = xmlParser.parseString(xmlString);
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
	
}
