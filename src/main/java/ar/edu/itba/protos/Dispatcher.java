package ar.edu.itba.protos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ar.edu.itba.admin.ProxyConfiguration;

public class Dispatcher {
	
	private Selector selector;
	private ClientProxyHandler clientProxyHandler;
	private Map<ServerSocketChannel, Handler> handlers;

	public void runProxy () throws IOException {
		
		/* Get configurations */
		String xmppProxyHost = ProxyConfiguration.getInstance().getProperty("xmpp_proxy_host");
		int xmppPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_port"));
		//int xmppAdminPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_admin_port"));


		/* Open selector */
        this.selector = Selector.open();
		
		/* Instance admin and client channels */
        clientProxyHandler = new ClientProxyHandler(xmppProxyHost, xmppPort, selector);
		//XMPPAdminProxy xmppAdminProxy = new XMPPAdminProxy(xmppProxyHost, xmppAdminPort, selector);
        
        /* Handlers & Channels */
        handlers = new HashMap<ServerSocketChannel, Handler>();
        handlers.put(clientProxyHandler.getProxyChannel(), clientProxyHandler);

        while (true) {
            // wait for events
            this.selector.select();

            //work on selected keys
            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();

                // this is necessary to prevent the same key from coming up 
                // again the next time around.
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                }
                else if (key.isReadable()) {
                    this.read(key);
                }
            }
        }
	

	}
	
	private void read(SelectionKey key) throws IOException {
		clientProxyHandler.read(key);
	}

	private void accept(SelectionKey key) throws IOException {
		clientProxyHandler.accept(key);
	}

}