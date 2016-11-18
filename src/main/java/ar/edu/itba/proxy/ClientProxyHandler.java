package ar.edu.itba.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;

import ar.edu.itba.filters.Multiplexing;
import ar.edu.itba.filters.SilentUser;
import ar.edu.itba.filters.Transformations;
import ar.edu.itba.logger.XMPPProxyLogger;
import ar.edu.itba.metrics.MetricsCollector;
import ar.edu.itba.stanza.Stanza;
import ar.edu.itba.utils.Utils;

public class ClientProxyHandler implements Handler {
	
	private XMPPProxyLogger logger;
    private InetSocketAddress listenAddress;
    private ConcurrentHashMap<SocketChannel, ProxyConnection> clientToProxyChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
    private ConcurrentHashMap<SocketChannel, ProxyConnection> proxyToClientChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
	private Selector selector;
    private final static String XMPP_FINAL_MESSAGE = "</stream:stream>";
    private final static int BUFFER_SIZE = 1024*100;
    private ServerSocketChannel channel;

    public ClientProxyHandler(String address, int port, Selector selector) throws IOException {
    	this.selector = selector;
    	listenAddress = new InetSocketAddress(address, port);
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(listenAddress);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    	logger = XMPPProxyLogger.getInstance();
        logger.info("Client proxy started");
    }

	public ServerSocketChannel getChannel() {
		return channel;
	}

	public void setChannel(ServerSocketChannel channel) {
		this.channel = channel;
	}

	/**
     * Accept connections to XMPP Proxy
     * @param key
     * @throws IOException
     */
    public void accept(SelectionKey key) throws IOException {
        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = keyChannel.accept();
        newChannel.configureBlocking(false);
        Socket socket = newChannel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        XMPPProxyLogger.getInstance().debug("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        newChannel.register(this.selector, SelectionKey.OP_READ);
        clientToProxyChannelMap.put(newChannel, new ProxyConnection(newChannel));
    }
    
    /**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
    public void read(SelectionKey key) throws IOException {
        SocketChannel keyChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int numRead = -1;
        numRead = keyChannel.read(buffer); // TODO catch java.io.IOException: Connection reset by peer
        String side = channelIsServerSide(keyChannel) ? "server" : "client";
        if (numRead == -1) {
            XMPPProxyLogger.getInstance().warn("Connection closed by " + side);
            keyChannel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead); 
        String stringRead = new String(data);
        if(MainProxy.verbose)
        	System.out.println(side + " wrote: " + stringRead);

        if (stringRead.equals(XMPP_FINAL_MESSAGE)) {
        	closeBothChannels(keyChannel);
        } else {
        	if (channelIsServerSide(keyChannel)) {
        		ProxyConnection connection = proxyToClientChannelMap.get(keyChannel);
        		getToJid(connection, stringRead);
        		if (connection.isUserRecognized() /*&& !isInitialMessage(stringRead)*/) {
        			if(!connection.initialMsgs.isEmpty())
        				sendToServer(connection.initialMsgs.poll(), connection);
        			else
        				sendToClient(stringRead, connection.getClientChannel());
        		} else if (connection.isTryingToRegister() && !isInitialMessage(stringRead)) {
        			// when registration happens, server sends 2 messages, client already got the first one from us when we faked it
        			sendToClient(stringRead, connection.getClientChannel());
        		}
            } else {
            	ProxyConnection connection = clientToProxyChannelMap.get(keyChannel);
            	if (!connection.isUserRecognized() && !userTryingToRegister(stringRead)) {
            		handleUnknownUser(connection, stringRead);
            	} else {
            		if (userTryingToRegister(stringRead) && !userIsSendingRegistrationValues(stringRead)) {
            			connection.setTryingToRegister(true);
            			sendToServer(Stanza.initialStanza(), connection);
            			sendToServer(stringRead, connection);
            		} else {
            			String fromJid = connection.getJid();
            			if (SilentUser.getInstance().filterMessage(stringRead, fromJid)) {
            				sendToClient(SilentUser.getInstance().getErrorMessage(fromJid), keyChannel);
            				XMPPProxyLogger.getInstance().info("Message from " + fromJid + " filtered");
            			} else {
            				sendToServer(stringRead, connection);
            			}
            		}
            	}
            }
        }
    }
    
    private boolean userIsSendingRegistrationValues(String stringRead) {
		return Utils.regexMatch(stringRead, "jabber:iq:register.*type=.submit");
	}

	private boolean isInitialMessage(String stringRead) {
		return Utils.regexMatch(stringRead, "<stream:stream.*xmlns=.jabber:client");
	}

	private boolean userTryingToRegister(String stringRead) {
		return Utils.regexMatch(stringRead, "register");
	}

	private void handleUnknownUser(ProxyConnection connection, String stringRead) {
		connection.initialMsgs.offer(stringRead);
		if (Utils.regexMatch(stringRead, Stanza.CLIENT_AUTH_PATTERN)) {
        	String auth64 = Utils.regexRead(stringRead, Stanza.CLIENT_AUTH_PATTERN).group(1);
        	byte[] authDecoded = Base64.decodeBase64(auth64);
        	int passwordStartIndex = new String(authDecoded).indexOf('\0', 2);
        	String username = new String(authDecoded, 1, passwordStartIndex - 1);
        	if (username != null){
        		System.out.println("Client " + username + " is attempting to connect");
        		XMPPProxyLogger.getInstance().info("Client " + username + " is attempting to connect");
        	}
        	connection.setUsername(username);
        	sendToServer(connection.initialMsgs.poll(),connection);
        	//sendToClient(Stanza.FAKE_PLAIN_AUTH_SUCCESS, connection.getClientChannel());
        } else {
        	sendToClient(Stanza.FAKE_SERVER_INITIAL_PLAIN_AUTH_RESPONSE, connection.getClientChannel());
        }
	}

	private void fakeToServer(ProxyConnection connection) {
		// TODO Auto-generated method stub
		
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
	    	XMPPProxyLogger.getInstance().debug("Client[" + clientLocalAddress + "] to XMPP Proxy[" + clientRemoteAddress + "] socket closed");
			XMPPProxyLogger.getInstance().info("Client " + jid + " has disconnected");
	    	proxy.getClientChannel().close();
	    	if(proxy.getServerChannel()!=null && proxy.getServerChannel().isOpen()){
	    		String serverLocalAddress = proxy.getServerChannel().getLocalAddress().toString();
				String serverRemoteAddress = proxy.getServerChannel().getRemoteAddress().toString();
	    		proxyToClientChannelMap.remove(proxy.getServerChannel());
	    		proxy.getServerChannel().close();
	    		XMPPProxyLogger.getInstance().debug("XMPP Proxy[" + serverLocalAddress + "] to XMPP Server[" + serverRemoteAddress + "] socket closed");
	    	}
    	} catch (IOException e) {
    		logger.error("Error closing server and client channels");
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
    private void sendToServer(String s, ProxyConnection pc) {
    	try {
	    	if (pc.getServerChannel() == null) {
	    		InetSocketAddress hostAddress = getClientSpecificXmppServerAddress(pc);
	            SocketChannel serverChannel = SocketChannel.open(hostAddress);
	            serverChannel.configureBlocking(false);
	            serverChannel.register(this.selector, SelectionKey.OP_READ);
	            pc.setServerChannel(serverChannel);
	            proxyToClientChannelMap.put(serverChannel, pc);
	    	}
	    	String newString = Transformations.getInstance().applyTransformations(s);
	        writeInChannel(newString, pc.getServerChannel());
    	}
        catch(ClosedByInterruptException e) {
        	logger.error(e.toString());
  	      	System.out.println("ClosedByInterruptException");
	    }
	    catch(AsynchronousCloseException e) {
	    	System.out.println("AsynchronousCloseException");
	    }
	    catch(UnresolvedAddressException e) {
	    	logger.error(e.toString());
	    	System.out.println("UnresolvedAddressException");
	    }
	    catch(UnsupportedAddressTypeException e) {
	    	logger.error(e.toString());
	    	System.out.println("UnsupportedAddressTypeException");
	    }
	    catch(SecurityException e) {
	    	logger.error(e.toString());
	    	System.out.println("SecurityException");
	    }
	    catch(IOException e) {
	    	logger.error(e.toString());
	    	System.out.println("IOException");
	    }
    }
    
    private InetSocketAddress getClientSpecificXmppServerAddress(
			ProxyConnection pc) {
    	InetSocketAddress serverTo = Multiplexing.getInstance().getUserServer(pc.getUsername());
		return serverTo;
	}

	/**
     * Write data to a specific channel
     */
    public void writeInChannel(String s, SocketChannel channel) {
    	ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
			channel.write(buffer);
		} catch (IOException e) {
			logger.warn("Connection closed by client");
			
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