package ar.edu.itba.logger;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.FileAppender;

public class XMPPProxyLogger {
	
	private static XMPPProxyLogger instance;
	private Logger logger;

	
	private XMPPProxyLogger() throws IOException {
		logger = Logger.getLogger(XMPPProxyLogger.class);
		logger.addAppender(new FileAppender(new PatternLayout("%5p | %d | %F | %L | %m%n"), "xmpp-server.log"));
	}
	
	public static XMPPProxyLogger getInstance() {
		try {
			if (instance == null)
				instance = new XMPPProxyLogger();
		} catch (IOException e) {
			System.out.println("No se pudo abrir el archivo");
		}
		return instance;
	}
	
	public void debug(String s) {
		logger.debug(s);
	}
	
	public void error(String s) {
		logger.error(s);
	}
}
