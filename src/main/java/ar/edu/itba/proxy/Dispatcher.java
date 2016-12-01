package ar.edu.itba.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ar.edu.itba.config.ProxyConfiguration;

public class Dispatcher {
	
	private Selector selector;
	private ClientProxyHandler clientProxyHandler;
	private AdminProxyHandler adminProxyHandler;
	private Map<ServerSocketChannel, Handler> handlers = new HashMap<ServerSocketChannel, Handler>(2); // only admin and client channels

	public void runProxy () throws IOException {
		
		/* Get configurations */
		String xmppProxyHost = ProxyConfiguration.getInstance().getProperty("xmpp_proxy_host");
		int xmppPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_port"));
		int xmppAdminPort = Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_admin_port"));


		/* Open selector */
        this.selector = Selector.open();
		
		/* Instance admin and client channels */
        clientProxyHandler = new ClientProxyHandler(xmppProxyHost, xmppPort, selector);
        adminProxyHandler = new AdminProxyHandler(xmppProxyHost, xmppAdminPort, selector);
        
        /* Handler mapping */
        handlers.put(clientProxyHandler.getChannel(), clientProxyHandler);
        handlers.put(adminProxyHandler.getChannel(), adminProxyHandler);
        
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
                } else if (key.isWritable()) {
                	this.write(key);
                }
            }
        }
	

	}
	
	private void write(SelectionKey key) throws IOException {
		Handler handler = getHandlerFromKey(key);
		handler.write(key);
	}

	private void read(SelectionKey key) throws IOException {
		Handler handler = getHandlerFromKey(key);
		handler.read(key);
	}

	private void accept(SelectionKey key) throws IOException {
		handlers.get(key.channel()).accept(key);
	}
	
	private Handler getHandlerFromKey(SelectionKey key) throws IOException {
		Handler handler = clientProxyHandler;
		SocketChannel socketChannel = (SocketChannel) key.channel();
		if (socketChannel.getLocalAddress().equals(adminProxyHandler.getChannel().getLocalAddress())) {
			handler = adminProxyHandler;
		}
		return handler;
	}
}