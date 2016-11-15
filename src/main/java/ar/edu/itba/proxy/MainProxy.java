package ar.edu.itba.proxy;

import java.io.IOException;

public class MainProxy {
	public static boolean verbose = false;
	public static void main(String[] args) throws IOException {
		
		if(args.length > 0){
			if(args[0].equals("-v"))
				verbose = true;
		}
		
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.runProxy();
	}

}
