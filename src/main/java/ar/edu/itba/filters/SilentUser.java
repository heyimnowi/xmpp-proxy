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
	
	public boolean filterMessage(String message, String fromJid) {
		return Stanza.isMessage(message) && Stanza.isChatMessage(message) && isSilent(fromJid);
	}
	
	public boolean isSilent(String jid) {
		return users.contains(jid);
	}	
}
