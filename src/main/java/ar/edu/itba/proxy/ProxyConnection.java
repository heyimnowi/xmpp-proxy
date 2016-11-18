package ar.edu.itba.proxy;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ProxyConnection {
	
	private SocketChannel clientChannel;
	private SocketChannel serverChannel;
	private String jid;
	private String username;
	private boolean tryingToRegister;
	
	public Queue<String> initialMsgs = new LinkedList<String>();
	
	public ProxyConnection(SocketChannel clientChannel) {
		this.clientChannel = clientChannel; 
	}
	
	public SocketChannel getClientChannel() {
		return clientChannel;
	}
	
	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}
	
	public SocketChannel getServerChannel() {
		return serverChannel;
	}
	
	public void setServerChannel(SocketChannel serverChannel) {
		this.serverChannel = serverChannel;
	}
	
	public String getJid() {
		return jid;
	}
	
	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isUserRecognized() {
		return this.username != null;
	}

	public boolean isTryingToRegister() {
		return tryingToRegister;
	}

	public void setTryingToRegister(boolean tryingToRegister) {
		this.tryingToRegister = tryingToRegister;
	}
}
