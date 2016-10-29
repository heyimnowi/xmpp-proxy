package ar.edu.itba.protos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.logger.XMPPProxyLogger;

public class AdminProxyHandler implements Handler {
	
	private InetSocketAddress listenAddress;
	private XMPPProxyLogger logger;
	private Selector selector;
	private final static int BUFFER_SIZE = 1024*100;
	private ServerSocketChannel adminChannel;
	
    public AdminProxyHandler(String address, int port, Selector selector) throws IOException {
    	System.out.println("Hola admin");
    	this.selector = selector;
    	listenAddress = new InetSocketAddress(address, port);
        adminChannel = ServerSocketChannel.open();
        adminChannel.configureBlocking(false);
        adminChannel.socket().bind(listenAddress);
        adminChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    	logger = XMPPProxyLogger.getInstance();
        logger.info("Admin proxy started");
    }
    
	public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            logger.warn("Connection closed by client");
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead); 
        String stringRead = new String(data);
        System.out.println("Got admin: " + stringRead);
		
	}


	
	public void writeInChannel(String s, SocketChannel channel) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public ServerSocketChannel getChannel() {
		return adminChannel;
	}

	public SocketChannel accept(SelectionKey key) throws IOException {
		ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = keyChannel.accept();
        newChannel.configureBlocking(false);
        Socket socket = newChannel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        XMPPProxyLogger.getInstance().debug("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        newChannel.register(this.selector, SelectionKey.OP_READ);
        return newChannel;
	}
}