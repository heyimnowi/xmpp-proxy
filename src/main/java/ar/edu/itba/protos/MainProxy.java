package ar.edu.itba.protos;

import java.io.IOException;

public class MainProxy {

	public static void main(String[] args) throws IOException {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.runProxy();
	}

}
