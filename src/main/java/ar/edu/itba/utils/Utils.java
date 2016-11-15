package ar.edu.itba.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	/**
	 * Get a match from a regex
	 * @param s
	 * @param pattern
	 * @return
	 */
	public static Matcher regexRead(String s, String pattern) {
		Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher m = r.matcher(s);
		m.find();
		return m;
	}
	
	/**
	 * Ask if a regex matchs
	 * @param s
	 * @param pattern
	 * @return
	 */
	public static boolean regexMatch(String s, String pattern) {
		Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher m = r.matcher(s);
		return m.find();
	}
}
