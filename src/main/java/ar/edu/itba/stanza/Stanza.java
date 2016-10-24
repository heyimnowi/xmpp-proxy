package ar.edu.itba.stanza;

public class Stanza {
	
	private static String MESSAGE_TAG = "<message";
	private static Stanza instance;
	
	public static Stanza getInstance() {
		if (instance == null) {
			instance = new Stanza();
		}
		return instance;
	}
	
	public boolean isMessage(String s){
		return s.contains(MESSAGE_TAG);
	}

}
