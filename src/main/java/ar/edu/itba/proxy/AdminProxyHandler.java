package ar.edu.itba.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import com.google.common.base.CaseFormat;

import ar.edu.itba.admin.AdminCommand;
import ar.edu.itba.admin.StatusResponse;
import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.filters.SilentUser;
import ar.edu.itba.logger.XMPPProxyLogger;
import ar.edu.itba.metrics.MetricsCollector;
import ar.edu.itba.proxy.AdminConnection.AdminState;
import ar.edu.itba.utils.Utils;

public class AdminProxyHandler implements Handler {
	
	private InetSocketAddress listenAddress;
	private XMPPProxyLogger logger;
	private Selector selector;
	private ProxyConfiguration config;
	private final static int BUFFER_SIZE = 1024*100;
	private static final String METRICS = "metrics";
	private static String adminUser;
	private static String adminPassword;
	private static String COMMAND_PATTERN = "[a-zA-Z]+";
	private static String KEY_PATTERN = "[a-zA-z\\-]+";
	private static String VALUE_PATTERN = ".+";
	private static String keyValRegex = "^(" + COMMAND_PATTERN + ")\\s+(" + KEY_PATTERN + ")\\s*=\\s*(" + VALUE_PATTERN + ")\n$";
	private static String onlyKeyRegex = "^(GET ?|LOGOUT$)\\s*("+ KEY_PATTERN +")?\n$";
	private static int COMMAND_GROUP = 1;
	private static int KEY_GROUP = 2;
	private static int VALUE_GROUP = 3;
    private ConcurrentHashMap<SocketChannel, AdminConnection> connections = new ConcurrentHashMap<SocketChannel, AdminConnection>();
	private ServerSocketChannel adminChannel;
	
    public AdminProxyHandler(String address, int port, Selector selector) throws IOException {
    	System.out.println("Hola admin");
		config = ProxyConfiguration.getInstance();
    	this.selector = selector;
    	listenAddress = new InetSocketAddress(address, port);
        adminChannel = ServerSocketChannel.open();
        adminChannel.configureBlocking(false);
        adminChannel.socket().bind(listenAddress);
        adminChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    	logger = XMPPProxyLogger.getInstance();
		adminUser = config.getProperty("admin_user");
		adminPassword = config.getProperty("admin_password");
        logger.info("Admin proxy started");
    }
    
    public ServerSocketChannel getChannel() {
    	return adminChannel;
    }
    
	public void accept(SelectionKey key) throws IOException {
		ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = keyChannel.accept();
        newChannel.configureBlocking(false);
        Socket socket = newChannel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        XMPPProxyLogger.getInstance().debug("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        newChannel.register(this.selector, SelectionKey.OP_READ);
        AdminConnection adminConnection = new AdminConnection(newChannel);
        connections.put(newChannel, adminConnection);
	}

	public void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		int numRead = -1;
		numRead = channel.read(buffer);

		if (numRead == -1) {
			logger.warn("Connection closed by client");
			channel.close();
			key.cancel();
			return;
		}

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead); 
		String stringRead = new String(data);
		System.out.println("Got admin: " + stringRead);
		
		AdminConnection connection = connections.get(channel);
		String response = handleAdminCommand(connection, stringRead);
		writeInChannel(response, channel);
	}

	private String handleAdminCommand(AdminConnection connection,
			String stringRead) {
		StatusResponse statusResponse = StatusResponse.INTERNAL_ERROR;
		try {
			ProxyConfiguration conf = ProxyConfiguration.getInstance();
			switch (connection.getState()) {
			case LOGGED_IN:
				if (Utils.regexMatch(stringRead, keyValRegex)) {
					Matcher commandGroup = Utils.regexRead(stringRead, keyValRegex);
					String command = commandGroup.group(COMMAND_GROUP);
					String key = commandGroup.group(KEY_GROUP);
					String value = commandGroup.group(VALUE_GROUP);
					
					switch (AdminCommand.valueOf(command.toUpperCase())) {
					case SET:
						conf.setProperty(key, value);
						statusResponse = StatusResponse.COMMAND_OK;
						break;
					case UNSET:
						conf.unsetProperty(key, value);
						statusResponse = StatusResponse.COMMAND_OK;
						break;
					default:
						statusResponse = StatusResponse.COMMAND_UNKNOWN;
						break;
					}
				} else if (Utils.regexMatch(stringRead, onlyKeyRegex)) {
					Matcher commandGroup = Utils.regexRead(stringRead, onlyKeyRegex);
					
					String command = commandGroup.group(COMMAND_GROUP);
					String key = commandGroup.group(KEY_GROUP);
					switch (AdminCommand.valueOf(command.toUpperCase().trim())) {
					case GET:
						if (key == null) {
							statusResponse = StatusResponse.COMMAND_OK;
							statusResponse.setExtendedMessage(conf.getAllProperties());
						} else if (key.equals(METRICS)) {
							// TODO imprimir metricas
						} else {
							String property = conf.getProperty(key);
							if (property.isEmpty()) {
								statusResponse = StatusResponse.NOT_FOUND;
							} else {
								statusResponse = StatusResponse.COMMAND_OK;
								statusResponse.setExtendedMessage(property);
							}
						}
						break;
					case LOGOUT:
						connection.setState(AdminState.NO_STATUS);
						connection.closeConnection();
						break;
					default:
						break;
					}
				} else {
					statusResponse = StatusResponse.COMMAND_UNKNOWN;
				}
				break;
			case NO_STATUS:
				if (Utils.regexMatch(stringRead, keyValRegex)) {
					Matcher commandGroup = Utils.regexRead(stringRead, keyValRegex);
					String command = commandGroup.group(COMMAND_GROUP);
					switch (AdminCommand.valueOf(command.toUpperCase())) {
					case LOGIN:
						String user = commandGroup.group(KEY_GROUP);
						String password = commandGroup.group(VALUE_GROUP);
						if (adminUser.equals(user) && adminPassword.equals(password)) {
							connection.setState(AdminState.LOGGED_IN);
							statusResponse = StatusResponse.LOGIN_OK;
						} else {
							statusResponse = StatusResponse.LOGIN_FAILED;
						}
						break;
					default:
						statusResponse = StatusResponse.COMMAND_FORBIDDEN;
						break;
					}
				} else {
					statusResponse = StatusResponse.COMMAND_FORBIDDEN;
				}
				
			}
			return statusResponse.toString();
		} catch (Exception e) {
			statusResponse = StatusResponse.INTERNAL_ERROR;
			return statusResponse.toString();
		}
	}

	public void writeInChannel(String s, SocketChannel channel)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
			channel.write(buffer);
		} catch (IOException e) {
			logger.warn("Connection closed by client");
			
		}
        buffer.clear();
	}
}