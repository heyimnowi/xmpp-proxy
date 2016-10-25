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
	
	/**
	 * Set a log with type DEBUG
	 * @param s
	 */
	public void debug(String s) {
		logger.debug(s);
	}
	
	/**
	 * Set a log with type ERROR
	 * @param s
	 */
	public void error(String s) {
		logger.error(s);
	}
	
	/**
	 * Set a log with type INFO
	 * @param s
	 */
	public void info(String s) {
		logger.info(s);
	}
	
	/**
	 * Set a log with type TRACE
	 * @param s
	 */
	public void trace(String s) {
		logger.trace(s);
	}
	
	/**
	 * Set a log with type WARN
	 * @param s
	 */
	public void warn(String s) {
		logger.warn(s);
	}
}
