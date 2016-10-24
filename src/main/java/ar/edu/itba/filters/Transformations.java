package ar.edu.itba.filters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.stanza.Stanza;

public class Transformations {
	
	private static Transformations instance;
	
	private static Map<Character,String> swaps;
	private static String LEFT_OF_BODY_PATTERN = "(.*)<body>";
	private static String BODY_PATTERN = "<body>(.*)<\\/body>";
	private static String RIGHT_OF_BODY = "<\\/body>(.*)";
	
	public static Transformations getInstance() {
		if (instance == null) {
			instance = new Transformations();
		}
		return instance;
	}
	
	private Transformations() {
		swaps = new HashMap<Character,String>();
		swaps.put('a', "4");
		swaps.put('e', "3");
		swaps.put('i', "1");
		swaps.put('o', "0");
		swaps.put('c', "&lt;");
		// Uppercase
		swaps.put('A', "4");
		swaps.put('E', "3");
		swaps.put('I', "1");
		swaps.put('O', "0");
		swaps.put('C', "&lt;");
	}
	
	public String applyTransformations(String s) {
		if (!Stanza.getInstance().isMessage(s)) {
			return s;
		}
		String leftOfBody = regexRead(s, LEFT_OF_BODY_PATTERN).group(0); // 0 means all the matched pattern
		String body = regexRead(s, BODY_PATTERN).group(1);
		String rightOfBody = regexRead(s, RIGHT_OF_BODY).group(0);
		String bodyUnescaped = StringEscapeUtils.unescapeXml(body);
		String bodyReEscaped = StringEscapeUtils.escapeXml11(swapCharacters(bodyUnescaped));
		return leftOfBody + bodyReEscaped + rightOfBody;
		
	}
	
	private String swapCharacters(String s) {
		StringBuffer buffer = null;
		if (s != null) {
			buffer = new StringBuffer(s.length());
			for (int i = 0; i < s.length(); i++) {
				if (swaps.containsKey(s.charAt(i))) {
					buffer.append(swaps.get(s.charAt(i)));
				} else {
					buffer.append(s.charAt(i));
				}
			}
		}
		return buffer.toString();
	}
	
	private Matcher regexRead(String s, String pattern) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(s);
		if (m.find( )) {
		     System.out.println("Found value: " + m.group(1) );
		  }	else {
		     System.out.println("NO MATCH");
		  }
		  return m;
	}
}
