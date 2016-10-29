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
	
	/**
	 * Ask if a particular message has to be filtered
	 * @param message
	 * @param fromJid
	 * @return
	 */
	public boolean filterMessage(String message, String fromJid) {
		return Stanza.isMessage(message) && Stanza.isChatMessage(message) && isSilent(fromJid);
	}
	
	/**
	 * Ask is an user is silent
	 * @param jid
	 * @return
	 */
	public boolean isSilent(String jid) {
		return users.contains(jid);
	}
	
	/**
	 * Get the XMPP error message when a user is silent 
	 * @param fromJid
	 * @return
	 */
	public String getErrorMessage(String fromJid) {
		return Stanza.errorMessage("not-allowed", "cancel", "405",fromJid, "Estas silenciado vieja");
	}
}
