package ar.edu.itba.filters;

import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.stanza.Stanza;

public class SilentUser implements ProxyFilter{
	
	private static SilentUser instance;
	private Set<String> users = null;
	private boolean enabled;
	
	private SilentUser() {
		users = new HashSet<String>();
		update();
	}
	
	public static SilentUser getInstance() {
		if (instance == null) {
			instance = new SilentUser();
		}
		return instance;
	}
	
	public void update() {
		users.clear();
		String stringUsers = ProxyConfiguration.getInstance().getProperty("silenceuser");
		for (String user : stringUsers.split(",")) {
			users.add(user);
		}
		this.enabled = Boolean.parseBoolean(ProxyConfiguration.getInstance().getProperty("silenceuser_enabled"));
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
	
	public void addUser(String user) {
		users.add(user);
	}
	
	public void deleteUser(String user) {
		users.remove(user);
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}
}
