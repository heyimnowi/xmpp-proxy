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
import ar.edu.itba.metrics.MetricsCollector;
import ar.edu.itba.stanza.Stanza;
import ar.edu.itba.utils.Utils;

public class XMPPProxy {
	
	private Selector selector;
    private InetSocketAddress listenAddress;
    private ConcurrentHashMap<SocketChannel, ProxyConnection> clientToProxyChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
    private ConcurrentHashMap<SocketChannel, ProxyConnection> proxyToClientChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
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
        clientToProxyChannelMap.put(channel, new ProxyConnection(channel));
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
        //System.out.println("Got: " + stringRead);
        
        if (stringRead.equals(XMPP_FINAL_MESSAGE)) {
        	closeBothChannels(channel);
        } else {
        	if (channelIsServerSide(channel)) {
        		getToJid(proxyToClientChannelMap.get(channel), stringRead);
            	sendToClient(stringRead, proxyToClientChannelMap.get(channel).getClientChannel());
            } else {
            	String fromJid = clientToProxyChannelMap.get(channel).getJid();
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
    	if (channelIsServerSide(channel)) {
    		closeProxy(proxyToClientChannelMap.get(channel));
    	}else{
    		closeProxy(clientToProxyChannelMap.get(channel));
    	}
    }
    
    private void closeProxy(ProxyConnection proxy){
    	try{
    		String clientLocalAddress = proxy.getClientChannel().getLocalAddress().toString();
			String clientRemoteAddress = proxy.getClientChannel().getRemoteAddress().toString();
			String jid = proxy.getJid();
    		clientToProxyChannelMap.remove(proxy.getClientChannel());
	    	proxy.getClientChannel().close();
	    	XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
			XMPPProxyLogger.getInstance().info("Client " + jid + " has disconnected");
	    	if(proxy.getServerChannel()!=null){
	    		String serverLocalAddress = proxy.getServerChannel().getLocalAddress().toString();
				String serverRemoteAddress = proxy.getServerChannel().getRemoteAddress().toString();
	    		proxyToClientChannelMap.remove(proxy.getServerChannel());
	    		proxy.getServerChannel().close();
	    		XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
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
    	ProxyConnection pc = clientToProxyChannelMap.get(clientChannel);
    	if (pc.getServerChannel() == null) {
    		InetSocketAddress hostAddress = new InetSocketAddress(ProxyConfiguration.getInstance().getProperty("xmpp_server_host"), 5222);
            SocketChannel serverChannel = SocketChannel.open(hostAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(this.selector, SelectionKey.OP_READ);
            pc.setServerChannel(serverChannel);
            proxyToClientChannelMap.put(serverChannel, pc);
    	}
    	String newString = Transformations.getInstance().applyTransformations(s);
        writeInChannel(newString, pc.getServerChannel());
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
    	String jid;
        if(channelIsServerSide(channel)){
        	jid = getToJid(proxyToClientChannelMap.get(channel), s);
        	MetricsCollector.getInstance().msgReceived(jid,s);
        }else{
        	jid = getFromJid(clientToProxyChannelMap.get(channel), s);
        	MetricsCollector.getInstance().msgSent(jid, s);
        }
        buffer.clear();
    }
    
    /**
     * Get the client jabber id at the "from" attribute
     * @param channel
     * @param stanza
     * @return
     */
    private String getFromJid(ProxyConnection proxy, String stanza) {
    	if(proxy == null)
    		return null;
    	if(proxy.getJid()==null)
    		proxy.setJid(getJid(stanza,"from"));
    	return proxy.getJid();
    }
    
    /**
     * Get the client jabber id at the "to" attribute
     * @param channel
     * @param stanza
     * @return
     */
    private String getToJid(ProxyConnection proxy, String stanza){
    	if(proxy == null)
    		return null;
    	if(proxy.getJid()==null)
    		proxy.setJid(getJid(stanza,"to"));
    	return proxy.getJid();
    }
    
    private String getJid(String stanza, String attr){
		if (Utils.regexMatch(stanza, attr + "=")) {
			String toAttr = Stanza.tagAttr(stanza, attr);
    		if (toAttr.contains("@")) {
    			String jid = toAttr.split("/")[0];
    			XMPPProxyLogger.getInstance().info("Client " + jid + " has connected");
    			return jid;
    		}
    		return null;
		}
		return null;
    }
}