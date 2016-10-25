package ar.edu.itba.protos;

import java.nio.channels.SocketChannel;

public class ProxyConnection {
	
	private SocketChannel clientChannel;
	private SocketChannel serverChannel;
	private String jid;
	
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
}
