package ar.edu.itba.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public interface Handler {

	public void read(SelectionKey key) throws IOException;
	public void write(SelectionKey key) throws IOException;
	public void accept(SelectionKey key) throws IOException;
	public void writeInChannel(String s, SocketChannel channel) throws IOException;
	public ServerSocketChannel getChannel();
}
