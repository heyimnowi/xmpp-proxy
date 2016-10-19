package ar.edu.itba.stanza;

public enum StanzaType {
	iq("iq"),
	presence("presence"),
	message("message");

	private String type;
	
	StanzaType(String type){
		this.type = type;
	}
	
	@Override
	public String toString() {
		return type;
	}
	
}
