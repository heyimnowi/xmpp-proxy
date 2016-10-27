package ar.edu.itba.protos;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import ar.edu.itba.admin.ProxyConfiguration;

public class Dispatcher {
	
	private Selector selector;

	public void runProxy () throws IOException {
		this.selector = Selector.open();
		
		/* Get configurations */
		String xmppProxyHost = ProxyConfiguration.getInstance().getProperty("xmpp_proxy_host");
		int xmppPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_port"));
		int xmppAdminPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_admin_port"));
		
		/* Instance admin and client channels */
		XMPPProxy xmppProxy = new XMPPProxy(xmppProxyHost, xmppPort, selector);
		//XMPPAdminProxy xmppAdminProxy = new XMPPAdminProxy(xmppProxyHost, xmppAdminPort, selector);

		while (true) {
			
			this.selector.selectedKeys();
				
			Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
			while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
				
				if (key.isAcceptable()) {
					//this.accept(key);
				}

				if (key.isReadable()) {
					//this.read(key);
				}

				if (key.isValid()) {
					continue;
				}


                // this is necessary to prevent the same key from coming up 
                // again the next time around.
				keys.remove();
			}
			
	}
	

}

}