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

import ar.edu.itba.filters.SilentUser;
import ar.edu.itba.filters.Transformations;
import ar.edu.itba.logger.XMPPProxyLogger;
import ar.edu.itba.stanza.Stanza;
import ar.edu.itba.utils.Utils;

import com.google.common.base.Optional;

public class SocketServer {
	private Selector selector;
    private InetSocketAddress listenAddress;
    
    private ConcurrentHashMap<SocketChannel, ProxyConnection> connectionsMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
    
    private ConcurrentHashMap<SocketChannel, Optional<SocketChannel>> clientToServerChannelMap = new ConcurrentHashMap<SocketChannel, Optional<SocketChannel>>();
    private ConcurrentHashMap<SocketChannel, SocketChannel> serverToClientChannelMap = new ConcurrentHashMap<SocketChannel, SocketChannel>();
    private SocketChannel serverChannelWillBeHere = null;
    
    // Constants
    private final static String XMPP_FINAL_MESSAGE = "</stream:stream>";
    
    public static void main(String[] args) throws Exception {
    	Runnable server = new Runnable() {
			public void run() {
				 try {
					new SocketServer("localhost", 5222).startServer();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		};
		
       new Thread(server).start();
    }

    public SocketServer(String address, int port) throws IOException {
    	listenAddress = new InetSocketAddress(address, port);
    }

    // create server channel	
    private void startServer() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // retrieve server socket and bind to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        
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

    // Accept a connection made to this channel's socket
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        XMPPProxyLogger.getInstance().debug("Accepted new client connection from " + remoteAddr);
        channel.register(this.selector, SelectionKey.OP_READ);
        connectionsMap.put(channel, new ProxyConnection(channel));
        clientToServerChannelMap.put(channel, Optional.fromNullable(serverChannelWillBeHere));
    }
    
    // Read from the socket channel
    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024*100);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            XMPPProxyLogger.getInstance().debug("Connection closed by client " + remoteAddr);
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
            	sendToClient(stringRead, serverToClientChannelMap.get(channel));
            } else {
            	String fromJid = getFromJid(channel, stringRead);
            	if (SilentUser.getInstance().filterMessage(stringRead, fromJid)) {
            		XMPPProxyLogger.getInstance().info("Message from " + fromJid + " filtered");
            	} else {
            		sendToServer(stringRead, channel);
            	}
            }
        }
    }
    
    private void closeBothChannels(SocketChannel channel) {
    	try {
	    	if (channelIsServerSide(channel)) {
	    		SocketChannel clientChannel = serverToClientChannelMap.get(channel);
    			String serverLocalAddress = channel.getLocalAddress().toString();
    			String serverRemoteAddress = channel.getRemoteAddress().toString();
	    		serverToClientChannelMap.remove(channel);
	    		channel.close();
	    		XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
	    		

    			String clientLocalAddress = clientChannel.getLocalAddress().toString();
    			String clientRemoteAddress = clientChannel.getRemoteAddress().toString();
	    		clientToServerChannelMap.remove(clientChannel);
				clientChannel.close();
				XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
				XMPPProxyLogger.getInstance().info("Client " + connectionsMap.get(channel).getJid() + " have disconnected");

	    	} else {
	    		Optional<SocketChannel> maybeServerChannel = clientToServerChannelMap.get(channel);
	    		if (maybeServerChannel.isPresent()) {
	    			SocketChannel serverChannel = maybeServerChannel.get();
	    			serverToClientChannelMap.remove(serverChannel);
	    			String serverLocalAddress = serverChannel.getLocalAddress().toString();
	    			String serverRemoteAddress = serverChannel.getRemoteAddress().toString();
	    			serverChannel.close();
	    			XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
	    		}
    			String clientLocalAddress = channel.getLocalAddress().toString();
    			String clientRemoteAddress = channel.getRemoteAddress().toString();
	    		clientToServerChannelMap.remove(channel);
	    		channel.close();
	    		XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
	    		XMPPProxyLogger.getInstance().info("Client " + connectionsMap.get(channel).getJid() + " have disconnected");
	    	}
    	} catch (IOException e) {
    		//System.out.println("no le gusto que cerremos los channels..");
    		XMPPProxyLogger.getInstance().error("Error closing server and client channels");
			e.printStackTrace();
		}
    }
    
    private boolean channelIsServerSide(SocketChannel channel) {
    	return serverToClientChannelMap.get(channel) != null; 
    }
    
    private void sendToClient(String s, SocketChannel channel) {
    	writeInChannel(s, channel);
    }
    
    private void sendToServer(String s, SocketChannel clientChannel) throws IOException {
    	if (!clientToServerChannelMap.get(clientChannel).isPresent()) {
    		InetSocketAddress hostAddress = new InetSocketAddress("ec2-54-69-136-236.us-west-2.compute.amazonaws.com", 5222);
            SocketChannel serverChannel = SocketChannel.open(hostAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(this.selector, SelectionKey.OP_READ);
            connectionsMap.get(clientChannel).setServerChannel(serverChannel);
            clientToServerChannelMap.put(clientChannel, Optional.of(serverChannel));
            serverToClientChannelMap.put(serverChannel, clientChannel);
    	}
    	String newString = Transformations.getInstance().applyTransformations(s);
        writeInChannel(newString, clientToServerChannelMap.get(clientChannel).get());
    }
    
    private void writeInChannel(String s, SocketChannel channel) {
    	ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
			channel.write(buffer);
		} catch (IOException e) {
			XMPPProxyLogger.getInstance().warn("Connection closed by client");
			
		}
        String clientOrServer = channelIsServerSide(channel) ? "server" : "client"; 
        //System.out.println("Escribiendo al " + clientOrServer + " xmpp..");
        buffer.clear();
    }
    
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
    	} else {
    		return connectionsMap.get(channel).getJid();
    	}
    	
    }
}