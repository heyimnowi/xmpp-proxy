package ar.edu.itba.protos;

import java.io.IOException;
import java.nio.channels.Selector;

import ar.edu.itba.admin.ProxyConfiguration;

public class Dispatcher {
	  
	public static void main (String[] args) throws IOException {
		/* Get configurations */
		String xmppProxyHost = ProxyConfiguration.getInstance().getProperty("xmpp_proxy_host");
		int xmppPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_port"));
		int xmppAdminPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_admin_port"));
		Selector selector = Selector.open();
		
		/* Instance admin and client channels */
		XMPPProxy xmppProxy = new XMPPProxy(xmppProxyHost, xmppPort, selector);
		XMPPAdminProxy xmppAdminProxy = new XMPPAdminProxy(xmppProxyHost, xmppAdminPort, selector);
		
		new Thread(xmppProxy.start());
		new Thread(xmppAdminProxy.start());
	}
	

}
