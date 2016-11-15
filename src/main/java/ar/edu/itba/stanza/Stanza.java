package ar.edu.itba.stanza;

import java.util.regex.Matcher;

import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.utils.Utils;

public class Stanza {
	
	public static final String FAKE_SERVER_INITIAL_PLAIN_AUTH_RESPONSE = "<?xml version='1.0'?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='' xmlns='jabber:client'><stream:features><mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>PLAIN</mechanism></mechanisms><auth xmlns='http://jabber.org/features/iq-auth'/></stream:features>";
	public static final String FAKE_PLAIN_AUTH_SUCCESS = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'></success>";
	public static final String CLIENT_AUTH_PATTERN = ">(.*)<\\/auth>";
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
		Matcher m = Utils.regexRead(tag, attr + "='([^']*)'|=\"([^\"]*)\"");
		if (m.group(1) != null && !m.group(1).isEmpty()) {
			return m.group(1);
		} else if (m.group(2) != null && !m.group(2).isEmpty())  {
			return m.group(2);
		} else {
			return "";
		}
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
		//String admin = ProxyConfiguration.getInstance().getProperty("admin");;
		return "<message to='"+jid+"' type='error'>" +
				"<body>"+ message +"</body>" +
				"<error code='"+ code +"' type='"+ type +"'>" +
					"<"+condition+" xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
				"</error>" + 
			"</message>";
	}
	
	public static String initialStanza() {
		// check optimization only to fetch config after update of config;
		return "<?xml version='1.0' ?><stream:stream to='" + ProxyConfiguration.getInstance().getProperty("xmpp_server_hostname") + "' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>";
	}
}
