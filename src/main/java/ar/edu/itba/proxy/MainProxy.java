package ar.edu.itba.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import ar.edu.itba.config.ProxyConfiguration;

public class MainProxy {

	public static void main(String[] args) throws IOException {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.runProxy();
	}

}