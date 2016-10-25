package ar.edu.itba.protos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import ar.edu.itba.logger.XMPPProxyLogger;
public class XMPPAdminProxy {
	
	private InetSocketAddress listenAddress;
	private XMPPProxyLogger logger;
	private Selector selector;
	
    public XMPPAdminProxy(String address, int port, Selector selector) throws IOException {
    	listenAddress = new InetSocketAddress(address, port);
    	logger = XMPPProxyLogger.getInstance();
    }
    

    Runnable start() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel proxyChannel = ServerSocketChannel.open();
        proxyChannel.configureBlocking(false);
        
        // retrieve server socket and bind to port
        proxyChannel.socket().bind(listenAddress);
        proxyChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        logger.info("Proxy started");

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
    
	private void read(SelectionKey key) {
		// TODO Auto-generated method stub
		
	}


	public void accept(SelectionKey key) throws IOException {
		logger.info("Admin connected");
	}
}