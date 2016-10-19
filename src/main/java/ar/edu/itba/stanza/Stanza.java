package ar.edu.itba.stanza;

public class Stanza {

	private StanzaType type;
	private boolean completed, rejected;
	private String xmlString = "";

	public Stanza() {
		this.setCompleted(false);
		this.rejected = false;
	}

	public Stanza(StanzaType type) {
		this();
		this.type = type;
	}

	public String getType() {
		return type.toString();
	}

	public void setType(StanzaType type) {
		this.type = type;
	}

	public void setXMLString(String xmlString) {
		this.setXmlString(xmlString);
	}

	public void complete() {
		this.setCompleted(true);
	}

	public void reject() {
		this.rejected = true;
	}
	
	public boolean isrejected() {
		return rejected;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getXmlString() {
		return xmlString;
	}

	public void setXmlString(String xmlString) {
		this.xmlString = xmlString;
	}
}
