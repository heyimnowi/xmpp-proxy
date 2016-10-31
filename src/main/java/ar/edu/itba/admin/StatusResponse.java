package ar.edu.itba.admin;

public class StatusResponse {

	public static String getStatus(String type, String code, String message) {
		return "<status type='" + type + "' code='" + code + "'>"+ message +"</status>\n";
	}
}
