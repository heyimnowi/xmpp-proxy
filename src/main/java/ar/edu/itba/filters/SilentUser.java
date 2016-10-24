package ar.edu.itba.filters;

import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.admin.ProxyConfiguration;

public class SilentUser {
	
	private static SilentUser instance;
	private Set<String> users = null;

	public static SilentUser getInstance() {
		if (instance == null)
			instance = new SilentUser();
		return instance;
	}
	
	private SilentUser() {
		users = new HashSet<String>();
		String stringUsers = ProxyConfiguration.getInstance().getProperty("silenceuser");
		for (String user : stringUsers.split(",")) {
			users.add(user);
		}
	}
	
	public boolean isSilent(String s) {
		for (String user : users) {
			if (s.contains(user))
				return true;
		}
		return false;
	}	
}
