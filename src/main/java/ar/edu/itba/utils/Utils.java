package ar.edu.itba.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	public static Matcher regexRead(String s, String pattern) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(s);
		m.find();
		return m;
	}
	
	public static boolean regexMatch(String s, String pattern) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(s);
		return m.find();
	}
}
