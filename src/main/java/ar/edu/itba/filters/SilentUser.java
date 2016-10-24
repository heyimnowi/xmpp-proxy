package ar.edu.itba.filters;

import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.admin.ProxyConfiguration;
import ar.edu.itba.stanza.Stanza;

public class SilentUser {
	
	private static SilentUser instance;
	private Set<String> users = null;
	
	private SilentUser() {
		users = new HashSet<String>();
		String stringUsers = ProxyConfiguration.getInstance().getProperty("silenceuser");
		for (String user : stringUsers.split(",")) {
			users.add(user);
		}
	}
	
	public static SilentUser getInstance() {
		if (instance == null) {
			instance = new SilentUser();
		}
		return instance;
	}
	
//	* jid esta silenciado 
//	jid no esta silenciado pero el to: esta silenciado
	public boolean filterMessage(String message, String fromJid) {
		if (Stanza.isMessage(message)) {
			if (isSilent(fromJid)) {
				return true;
			} else {
				String toJid = Stanza.tagAttr(message, "to");
				// contemplar el jid/resource
				toJid = toJid.split("/")[0];
				return isSilent(toJid);
			}
		}
		return false;
	}
	
	public boolean isSilent(String jid) {
		return users.contains(jid);
	}	
}
