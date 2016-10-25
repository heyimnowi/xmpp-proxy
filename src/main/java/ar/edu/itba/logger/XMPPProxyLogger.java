package ar.edu.itba.logger;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XMPPProxyLogger {
	
	private static XMPPProxyLogger instance;
	private Logger logger;

	
	private XMPPProxyLogger() throws IOException {
		logger = LoggerFactory.getLogger(XMPPProxyLogger.class);
	}
	
	public static XMPPProxyLogger getInstance() {
		try {
			if (instance == null)
				instance = new XMPPProxyLogger();
		} catch (IOException e) {
			XMPPProxyLogger.getInstance().error("Cannot open logger configuration file");
		}
		return instance;
	}
	
	public void debug(String s) {
		logger.debug(s);
	}
	
	public void error(String s) {
		logger.error(s);
	}
	
	public void info(String s) {
		logger.info(s);
	}
	
	public void trace(String s) {
		logger.trace(s);
	}
	
	public void warn(String s) {
		logger.warn(s);
	}
}
