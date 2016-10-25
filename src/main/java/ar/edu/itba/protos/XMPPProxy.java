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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import ar.edu.itba.admin.ProxyConfiguration;
import ar.edu.itba.filters.SilentUser;
import ar.edu.itba.filters.Transformations;
import ar.edu.itba.logger.XMPPProxyLogger;
import ar.edu.itba.stanza.Stanza;
import ar.edu.itba.utils.Utils;

import com.google.common.base.Optional;

public class XMPPProxy {
	
	private Selector selector;
    private InetSocketAddress listenAddress;
    private SocketChannel serverChannelWillBeHere = null;
    private ConcurrentHashMap<SocketChannel, ProxyConnection> connectionsMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
    private ConcurrentHashMap<SocketChannel, Optional<SocketChannel>> clientToProxyChannelMap = new ConcurrentHashMap<SocketChannel, Optional<SocketChannel>>();
    private ConcurrentHashMap<SocketChannel, SocketChannel> proxyToClientChannelMap = new ConcurrentHashMap<SocketChannel, SocketChannel>();
    private final static String XMPP_FINAL_MESSAGE = "</stream:stream>";
    private final static int BUFFER_SIZE = 1024*100;
    
    public static void main(String[] args) throws Exception {
    	Runnable xmppProxy = new Runnable() {
			public void run() {
				 try {
					new XMPPProxy(
							ProxyConfiguration.getInstance().getProperty("xmpp_proxy_host"),
							Integer.parseInt(ProxyConfiguration.getInstance().getProperty("xmpp_proxy_port"))).startXMPPProxy();
				} catch (IOException e) {
					XMPPProxyLogger.getInstance().error("Cannot start proxy");
				}	
			}
		};	
       new Thread(xmppProxy).start();
    }

    public XMPPProxy(String address, int port) throws IOException {
    	listenAddress = new InetSocketAddress(address, port);
    }

    /**
     * Start the XMPP Proxy
     * @throws IOException
     */
    private void startXMPPProxy() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel proxyChannel = ServerSocketChannel.open();
        proxyChannel.configureBlocking(false);
        
        // retrieve server socket and bind to port
        proxyChannel.socket().bind(listenAddress);
        proxyChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        XMPPProxyLogger.getInstance().info("Proxy started");

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

    /**
     * Accept connections to XMPP Proxy
     * @param key
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel proxyChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = proxyChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        XMPPProxyLogger.getInstance().debug("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        channel.register(this.selector, SelectionKey.OP_READ);
        connectionsMap.put(channel, new ProxyConnection(channel));
        clientToProxyChannelMap.put(channel, Optional.fromNullable(serverChannelWillBeHere));
    }
    
    /**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            XMPPProxyLogger.getInstance().warn("Connection closed by client");
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead); 
        String stringRead = new String(data);
        System.out.println("Got: " + stringRead);
        
        if (stringRead.equals(XMPP_FINAL_MESSAGE)) {
        	closeBothChannels(channel);
        } else {
        	if (channelIsServerSide(channel)) {
            	sendToClient(stringRead, proxyToClientChannelMap.get(channel));
            } else {
            	String fromJid = getFromJid(channel, stringRead);
            	if (SilentUser.getInstance().filterMessage(stringRead, fromJid)) {
            		sendToClient(SilentUser.getInstance().getErrorMessage(fromJid), channel);
            		XMPPProxyLogger.getInstance().info("Message from " + fromJid + " filtered");
            	} else {
            		sendToServer(stringRead, channel);
            	}
            }
        }
    }
    
    /**
     * Close client and server channels
     * @param channel
     */
    private void closeBothChannels(SocketChannel channel) {
    	try {
	    	if (channelIsServerSide(channel)) {
	    		SocketChannel clientChannel = proxyToClientChannelMap.get(channel);
    			String serverLocalAddress = channel.getLocalAddress().toString();
    			String serverRemoteAddress = channel.getRemoteAddress().toString();
	    		proxyToClientChannelMap.remove(channel);
	    		channel.close();
	    		XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
	    		
    			String clientLocalAddress = clientChannel.getLocalAddress().toString();
    			String clientRemoteAddress = clientChannel.getRemoteAddress().toString();
	    		clientToProxyChannelMap.remove(clientChannel);
				clientChannel.close();
				XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
				XMPPProxyLogger.getInstance().info("Client " + connectionsMap.get(channel).getJid() + " have disconnected");

	    	} else {
	    		Optional<SocketChannel> maybeServerChannel = clientToProxyChannelMap.get(channel);
	    		if (maybeServerChannel.isPresent()) {
	    			SocketChannel serverChannel = maybeServerChannel.get();
	    			proxyToClientChannelMap.remove(serverChannel);
	    			String serverLocalAddress = serverChannel.getLocalAddress().toString();
	    			String serverRemoteAddress = serverChannel.getRemoteAddress().toString();
	    			serverChannel.close();
	    			XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
	    		}
    			String clientLocalAddress = channel.getLocalAddress().toString();
    			String clientRemoteAddress = channel.getRemoteAddress().toString();
	    		clientToProxyChannelMap.remove(channel);
	    		channel.close();
	    		XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
	    		XMPPProxyLogger.getInstance().info("Client " + connectionsMap.get(channel).getJid() + " have disconnected");
	    	}
    	} catch (IOException e) {
    		XMPPProxyLogger.getInstance().error("Error closing server and client channels");
			e.printStackTrace();
		}
    }
    
    /**
     * Ask if a channel is server side
     * @param channel
     * @return
     */
    private boolean channelIsServerSide(SocketChannel channel) {
    	return proxyToClientChannelMap.get(channel) != null; 
    }
    
    /**
     * Send data to XMPP Client
     * @param s
     * @param channel
     */
    private void sendToClient(String s, SocketChannel channel) {
    	writeInChannel(s, channel);
    }
    
    /**
     * Send data to XMPP Server
     * @param s
     * @param clientChannel
     * @throws IOException
     */
    private void sendToServer(String s, SocketChannel clientChannel) throws IOException {
    	if (!clientToProxyChannelMap.get(clientChannel).isPresent()) {
    		InetSocketAddress hostAddress = new InetSocketAddress(ProxyConfiguration.getInstance().getProperty("xmpp_server_host"), 5222);
            SocketChannel serverChannel = SocketChannel.open(hostAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(this.selector, SelectionKey.OP_READ);
            connectionsMap.get(clientChannel).setServerChannel(serverChannel);
            clientToProxyChannelMap.put(clientChannel, Optional.of(serverChannel));
            proxyToClientChannelMap.put(serverChannel, clientChannel);
    	}
    	String newString = Transformations.getInstance().applyTransformations(s);
        writeInChannel(newString, clientToProxyChannelMap.get(clientChannel).get());
    }
    
    /**
     * Write data to a specific channel
     */
    private void writeInChannel(String s, SocketChannel channel) {
    	ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
			channel.write(buffer);
		} catch (IOException e) {
			XMPPProxyLogger.getInstance().warn("Connection closed by client");
			
		}
        buffer.clear();
    }
    
    /**
     * Get the client jabber id from a connection
     * @param channel
     * @param stanza
     * @return
     */
    private String getFromJid(SocketChannel channel, String stanza) {
    	if (connectionsMap.get(channel).getJid() == null) {
    		if (Utils.regexMatch(stanza, "to=")) {
    			String toAttr = Stanza.tagAttr(stanza, "to");
        		if (toAttr.contains("@")) {
        			String jid = toAttr.split("/")[0];
        			connectionsMap.get(channel).setJid(jid);
        			XMPPProxyLogger.getInstance().info("Client " + jid + " have connected");
        			return jid;
        		}
    		}
    		return "";
    	}
    	return connectionsMap.get(channel).getJid();
    }
}