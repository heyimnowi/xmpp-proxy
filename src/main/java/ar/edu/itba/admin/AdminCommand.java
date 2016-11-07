package ar.edu.itba.admin;

public enum AdminCommand {
	
	LOGIN("login"),
	LOGOUT("logout"),
	SET("set"),
	UNSET("unset"),
	GET("get");
	
	private String message;
	
	private AdminCommand(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}


