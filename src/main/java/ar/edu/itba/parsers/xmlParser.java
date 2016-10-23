package ar.edu.itba.parsers;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class xmlParser {
	
	public static String parse (String s) {

	    try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(s);
			doc.getDocumentElement().normalize();
			
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
	
			NodeList body = doc.getElementsByTagName("body");
			if (body != null) {
				System.out.println(body);
				return body.toString();
			}
			return null;
			
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
		return null;
	  }

}
