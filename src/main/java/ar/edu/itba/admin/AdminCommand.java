package ar.edu.itba.admin;

import org.apache.commons.lang3.StringUtils;

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

	public String getLogtMessage() {
		String logMessage = StringUtils.rightPad(message, 6, ' ');
		return logMessage.toUpperCase();
	}

	public String getMessage() {
		return message.toUpperCase();
	}
}


