package ar.edu.itba.stanza;

import ar.edu.itba.utils.Utils;

public class Stanza {
	
	public static String MESSAGE_TAG = "<message";
	public static String BODY_TAG = "<body";
	public static String JID_TAG_PATTERN = "<jid>(.*)<\\/jid>";
	
	public static boolean isMessage(String s){
		return s.contains(MESSAGE_TAG);
	}
	
	public static String tagAttr(String tag, String attr) {
		return Utils.regexRead(tag, attr + "='([^']*)'").group(1);
	}

	public static boolean isChatMessage(String s) {
		return s.contains(BODY_TAG);
	}

}
