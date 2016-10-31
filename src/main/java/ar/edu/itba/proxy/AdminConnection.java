package ar.edu.itba.proxy;

import java.nio.channels.SocketChannel;

public class AdminConnection {
	
	private boolean logged;
	private boolean hello;
	private SocketChannel channel;
	
	public AdminConnection(SocketChannel channel) {
		this.logged = false;
		this.hello = false;
	}

	public boolean isLogged() {
		return logged;
	}

	public void setLogged(boolean logged) {
		this.logged = logged;
	}

	public boolean isHello() {
		return hello;
	}

	public void setHello(boolean hello) {
		this.hello = hello;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}

}
