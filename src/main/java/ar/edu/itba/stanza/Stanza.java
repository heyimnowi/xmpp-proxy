package ar.edu.itba.stanza;

import ar.edu.itba.admin.ProxyConfiguration;
import ar.edu.itba.utils.Utils;

public class Stanza {
	
	public static String MESSAGE_TAG = "<message";
	public static String BODY_TAG = "<body";
	public static String JID_TAG_PATTERN = "<jid>(.*)<\\/jid>";
	
	/**
	 * Ask if a stanza is type message
	 * @param s
	 * @return
	 */
	public static boolean isMessage(String s){
		return s.contains(MESSAGE_TAG);
	}
	
	/**
	 * Get an attribute from a stanza tag
	 * @param tag
	 * @param attr
	 * @return
	 */
	public static String tagAttr(String tag, String attr) {
		return Utils.regexRead(tag, attr + "='([^']*)'").group(1);
	}

	/**
	 * Ask if a message is type chat
	 * @param
	 * @return
	 */
	public static boolean isChatMessage(String s) {
		return s.contains(BODY_TAG);
	}
	
	public static String errorMessage(String condition, String type, String code,
			String jid, String message) {
		String admin = ProxyConfiguration.getInstance().getProperty("admin");;
		return "<message type='error'>" +
				"<body>"+ message +"</body>" +
				"<error code='"+ code +"' type='"+ type +"'>" +
					"<"+condition+" xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
				"</error>" + 
			"</message>";
	}

}
