package ar.edu.itba.admin;

public enum StatusResponse {
	
	LOGIN_OK(201, "Login succesfull"),
	LOGIN_FAILED(401, "User/Password incorrect"),
	COMMAND_OK(200, "Configuration updated"),
	COMMAND_UNKNOWN(400, "Command Unknown"),
	COMMAND_FORBIDDEN(402, "Command forbidden"),
	NOT_FOUND(403, "Not found"),
	INTERNAL_ERROR(500, "Internal Error");
	
	private int code;
	private String message;
	private String extendedMessage;
	
	private StatusResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
	
	public void setExtendedMessage(String message) {
		this.extendedMessage = message;
	}
	public String toString() {
		String text = extendedMessage == null ? message : extendedMessage;
		extendedMessage = null;
		return code + " - " + text + "\n";
	}
}
