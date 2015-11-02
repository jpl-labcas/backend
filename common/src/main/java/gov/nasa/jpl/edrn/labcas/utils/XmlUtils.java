package gov.nasa.jpl.edrn.labcas.utils;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

/**
 * Class containing general XML utility methods.
 * 
 * @author luca
 */
public class XmlUtils {
		
	private static final Logger LOG = Logger.getLogger(XmlUtils.class.getName());
	
	/**
	 * Factory method to create a new XML document object.
	 * @return
	 */
	public static final Document newXmlDocument() throws Exception {
		
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document xmlDocument = builder.newDocument();

        return xmlDocument;
	}

	/**
	 * Method to transform an XML document into a pretty-formatted string.
	 * @param xml
	 * @return
	 * @throws Exception
	 */
	public static final String xmlToString(Document xml) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		return out.toString();
	}
	
	/**
	 * Method to write an XML document to a file, will override existing file.
	 * @param file
	 * @throws Exception
	 */
	public static final void xmlToFile(Document xmldoc, File file) throws Exception {
		
		// pretty formatting
		String xmlstring = xmlToString(xmldoc);
		
		// write out
		FileUtils.writeStringToFile(file, xmlstring);
		
	}

}
