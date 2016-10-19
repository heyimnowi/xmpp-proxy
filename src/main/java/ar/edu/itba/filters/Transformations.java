package ar.edu.itba.filters;

import java.util.HashMap;
import java.util.Map;

public class Transformations {
	
	private static Map<Character,String> swaps;
	
	public Transformations() {
		swaps = new HashMap<Character,String>();
		swaps.put('a', "4");
		swaps.put('e', "3");
		swaps.put('i', "1");
		swaps.put('o', "0");
		swaps.put('c', "&lt;");
	}
	
	public static String applyTransformations(String s) {
		StringBuffer buffer = null;
		if (s != null) {

			for (int i = 0; i < s.length(); i++) {
				buffer = new StringBuffer(s.length());
				if (swaps.containsKey(s.charAt(i)))
					buffer.append(swaps.get(s.charAt(i)));
				else
					buffer.append(s.charAt(i));
			}
		}
		return buffer.toString();
	}

}
