package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Utility class to handle XML documents.
 */
public class XmlParser {
	

	/**
	 * Flag to validate the XML document.
	 */
	private final boolean validate;

	/**
	 * Constructor.
	 */
	public XmlParser(final boolean validate) {
		this.validate = validate;
	}
	
	/**
	 * Method to parse an XML string into a JDOM document.
	 * @param xml
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public Document parseString(final String xml) throws IOException, JDOMException {
		final StringReader sr = new StringReader(xml);
		return this.getBuilder().build(sr); 
	}

	/**
	 * Method to parse an XML file into a JDOM document.
	 * @param filepath
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public Document parseFile(final String filepath) throws IOException, JDOMException {
		return this.getBuilder().build(filepath);
	}
	
	/**
     * Method to parse an XML file into a JDOM document.
     * @param filepath
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    public Document parseFile(final File file) throws IOException, JDOMException {
        return this.getBuilder().build(file);
    }
	
	/**
	 * Method to obtain an XML parser.
	 * Note: the XML parser is NOT thread-safe, so it must be re-instantiated every time.
	 */
	private SAXBuilder getBuilder() {
		
		final SAXBuilder builder = new SAXBuilder(); 
		builder.setValidation(validate); 
		builder.setIgnoringElementContentWhitespace(true); 
		return builder;
		
	}

}
