package ar.edu.itba.config;

import java.io.FileInputStream;
import java.util.Properties;

import ar.edu.itba.logger.XMPPProxyLogger;

public class ProxyConfiguration {
	private Properties properties;
	private FileInputStream file;

	private static ProxyConfiguration instance;

	public static ProxyConfiguration getInstance() {
		if (instance == null)
			instance = new ProxyConfiguration();
		return instance;
	}

	private ProxyConfiguration() {
		this.properties = new Properties();
		try {
			String current = new java.io.File(".").getCanonicalPath();
			this.file = new FileInputStream(
					current
							+ "/src/main/java/ar/edu/itba/config/config.properties");
			properties.load(file);

		} catch (Exception e) {
			XMPPProxyLogger.getInstance().error("Cannot open proxy configuration file");
		}
	}

	public String getProperty(String property) {
		if (properties.get(property) == null)
			return "";
		return properties.get(property).toString();
	}

	public void setProperty(String property, String value) {
		properties.setProperty(property, value);
	}

	public boolean hasProperty(String property) {
		return properties.containsKey(property);
	}
}
