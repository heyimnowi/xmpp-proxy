package ar.edu.itba.filters;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.logger.XMPPProxyLogger;

public class Multiplexing {
	private InetSocketAddress defaultServer;
	private Map<String, InetSocketAddress> usernameToServerMap = new HashMap<String, InetSocketAddress>();;
	private static final int XMPP_DEFAULT_PORT = 5222;
	private static final String SPLIT_USERS_DELIMITER = ",";
	private static final String XMPP_DOMAIN_DELIMITER = "@";
	private static final String XMPP_PORT_DELIMITER = ":";

	private static Multiplexing instance = null;

	public static Multiplexing getInstance() {
		if (instance == null)
			instance = new Multiplexing();
		return instance;
	}

	/* From configuration file */
	private Multiplexing() {
		ProxyConfiguration conf = ProxyConfiguration.getInstance();
		defaultServer = new InetSocketAddress(conf.getProperty("xmpp_server_host"), Integer.parseInt(conf.getProperty("xmpp_server_port")));
		updateMultiplexedUsers();
	}

	/* From admin changes */
	public void updateMultiplexedUsers() {
		String multiplexedUsers = ProxyConfiguration.getInstance().getProperty("multiplexing").replaceAll(" ", "");
		if (multiplexedUsers != null && !multiplexedUsers.equals("")) {
			for (String s : multiplexedUsers.split(SPLIT_USERS_DELIMITER)) {
				String[] address = s.split(XMPP_DOMAIN_DELIMITER);
				String[] domain = address[1].split(XMPP_PORT_DELIMITER);
				String username = address[0];
				int port = domain.length == 1 ? XMPP_DEFAULT_PORT : Integer.parseInt(domain[1]);
				InetSocketAddress customXmppServer = new InetSocketAddress(domain[0], port);
				usernameToServerMap.put(username, customXmppServer);
			}
		}
	}

	public InetSocketAddress getUserServer(String user) {
		InetSocketAddress serverForUser = defaultServer;
		if (usernameToServerMap.containsKey(user))
			serverForUser = usernameToServerMap.get(user);
		if (!serverForUser.equals(defaultServer))
			XMPPProxyLogger.getInstance().info("Multiplexing " + user + " towards " + serverForUser);
		return serverForUser;
	}
}
