package gov.nasa.jpl.labcas.data_access_api.utils;

import java.io.IOException;

import org.jdom.JDOMException;

/** 
 * Utility class for writing XML to output. It contains only static methods.
 */
public class Serializer {

   /**
    * Method to write a JDOM document to file
    * @param jdoc the JDOM document
    * @param outputFile the intended xml output file
    * @exception IOException
    */
   public static void JDOMtoFile(org.jdom.Document jdoc, String outputFile)
                      throws java.io.IOException {

      org.jdom.output.XMLOutputter outputter = getXMLOutputter();
      java.io.FileWriter writer = new java.io.FileWriter(outputFile);
      outputter.output(jdoc, writer);
      writer.close();

   } // JDOMtoFile()

   /** 
    * Method to deserialize a JDOM document from a  String and write it to a file 
    * @param xml the String containig the serialized xml document
    * @param outputFile the intended xml output file
    * @exception IOException
    * @exception JDOMException
    */
   public static void JDOMtoFile(String xml, String outputFile)
                      throws java.io.IOException, org.jdom.JDOMException {

      java.io.StringReader sr = new java.io.StringReader(xml);
      org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
      org.jdom.Document jdoc =  sb.build(sr);
      org.jdom.output.XMLOutputter outputter = getXMLOutputter();
      java.io.FileWriter writer = new java.io.FileWriter(outputFile);
      outputter.output(jdoc, writer);
      writer.close();

   } // JDOMtoFile
   
   private static org.jdom.output.XMLOutputter getXMLOutputter() {
   	org.jdom.output.Format format = org.jdom.output.Format.getPrettyFormat();
   	format.setLineSeparator(System.getProperty("line.separator"));
   	org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter(format);
   	return outputter;
   } // getXMLOutputter()

   /**
    * Method to write a JDOM document to System.out
    * @param jdoc the JDOM document
    * @exception IOException
    */
   public static void JDOMout(org.jdom.Document jdoc) 
                      throws java.io.IOException {

      org.jdom.output.XMLOutputter outputter = getXMLOutputter();
      outputter.output(jdoc, System.out);

   } // JDOMout()

   /** 
    * Method to deserialize a JDOM document from a  String and write it to System.out
    * @param xml the String containig the serialized xml document
    * @exception IOException
    * @exception JDOMException
    */
   public static void JDOMout(String xml) 
          throws java.io.IOException, org.jdom.JDOMException {

      java.io.StringReader sr = new java.io.StringReader(xml);
      org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
      org.jdom.Document jdoc =  sb.build(sr);
      org.jdom.output.XMLOutputter outputter = getXMLOutputter();
      outputter.output(jdoc, System.out);

   } // JDOMtoFile

  /**
   * Method to serialize a JDOM document to a string representation
   * @param jdoc : XML document as org.jdom.Document object
   */
  public static String JDOMtoString(org.jdom.Document jdoc) throws org.jdom.JDOMException {
    return JDOMtoString(jdoc, false); // compact form
  } // JDOMtoString()
  
  /**
   * Method to serialize a JDOM document to a string representation
   * @param jdoc : XML document as org.jdom.Document object
   * @param pretty : true to output XML in pretty format
   */
  public static String JDOMtoString(org.jdom.Document jdoc, boolean pretty) throws org.jdom.JDOMException {
  	org.jdom.output.Format format = (pretty ? org.jdom.output.Format.getPrettyFormat() 
  			                                    : org.jdom.output.Format.getCompactFormat());
  	  org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter(format);
    return outputter.outputString(jdoc);
  } // JDOMtoString()  

  /**
   * Method to serialize a JDOM element to a string representation
   * @param jelem : XML element as JDOM Element object
   */
  public static String JDOMtoString(org.jdom.Element jelem) throws org.jdom.JDOMException {
    //org.jdom.output.XMLOutputter outputter= new org.jdom.output.XMLOutputter(org.jdom.output.Format.getCompactFormat());
    org.jdom.output.XMLOutputter outputter= new org.jdom.output.XMLOutputter();
    return outputter.outputString(jelem);
  } // JDOMtoString()

} // class Serializer
