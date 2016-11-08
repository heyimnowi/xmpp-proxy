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
import ar.edu.itba.admin.AdminCommand;
import ar.edu.itba.admin.StatusResponse;
import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.logger.XMPPProxyLogger;
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
        XMPPProxyLogger.getInstance().debug("Accepted new admin connection from " + localAddr + " to " + remoteAddr);
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
					String logMessage = AdminCommand.valueOf(command.toUpperCase()).getMessage();
					switch (AdminCommand.valueOf(command.toUpperCase())) {
					case SET:
						
						conf.setProperty(key, value);
						statusResponse = StatusResponse.COMMAND_OK;
						logger.info(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - Updated " + key + " configuration");
						break;
					case UNSET:
						conf.unsetProperty(key, value);
						statusResponse = StatusResponse.COMMAND_OK;
						logger.info(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - Updated " + key + " configuration");
						break;
					default:
						statusResponse = StatusResponse.COMMAND_UNKNOWN;
						logger.error(StatusResponse.COMMAND_UNKNOWN.getCode() + " - Bad request");
						break;
					}
				} else if (Utils.regexMatch(stringRead, onlyKeyRegex)) {
					Matcher commandGroup = Utils.regexRead(stringRead, onlyKeyRegex);
					
					String command = commandGroup.group(COMMAND_GROUP);
					String key = commandGroup.group(KEY_GROUP);
					String logMessage = AdminCommand.valueOf(command.toUpperCase().trim()).getMessage();
					switch (AdminCommand.valueOf(command.toUpperCase().trim())) {
					case GET:
						if (key == null) {
							statusResponse = StatusResponse.COMMAND_OK;
							statusResponse.setExtendedMessage(conf.getAllProperties());
							logger.info(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - all configurations");
						} else if (key.equals(METRICS)) {
							statusResponse = StatusResponse.COMMAND_OK;
							// TODO imprimir metricas
							statusResponse.setExtendedMessage(conf.getAllProperties());
							logger.info(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - metrics");
						} else {
							String property = conf.getProperty(key);
							if (property.isEmpty()) {
								statusResponse = StatusResponse.NOT_FOUND;
								logger.error(logMessage + " " + StatusResponse.NOT_FOUND.getCode() + " - " + key + " configuration not found");
							} else {
								statusResponse = StatusResponse.COMMAND_OK;
								statusResponse.setExtendedMessage(property);
								logger.info(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - " + key + " configuration");
							}
						}
						break;
					case LOGOUT:
						statusResponse = StatusResponse.COMMAND_OK;
						logger.debug(logMessage + " " + StatusResponse.COMMAND_OK.getCode() + " - Connection closed by admin");
						connection.setState(AdminState.NO_STATUS);
						connection.closeConnection();
						break;
					default:
						break;
					}
				} else {
					statusResponse = StatusResponse.COMMAND_UNKNOWN;
					logger.error(StatusResponse.COMMAND_UNKNOWN.getCode() + " - Bad request");
				}
				break;
			case NO_STATUS:
				if (Utils.regexMatch(stringRead, keyValRegex)) {
					Matcher commandGroup = Utils.regexRead(stringRead, keyValRegex);
					String command = commandGroup.group(COMMAND_GROUP);
					String logMessage = AdminCommand.valueOf(command.toUpperCase()).getMessage();
					switch (AdminCommand.valueOf(command.toUpperCase())) {
					case LOGIN:
						String user = commandGroup.group(KEY_GROUP);
						String password = commandGroup.group(VALUE_GROUP);
						if (adminUser.equals(user) && adminPassword.equals(password)) {
							connection.setState(AdminState.LOGGED_IN);
							statusResponse = StatusResponse.LOGIN_OK;
							logger.info(logMessage + " " + StatusResponse.LOGIN_OK.getCode() + " - Login successfull");
						} else {
							statusResponse = StatusResponse.LOGIN_FAILED;
							logger.info(logMessage + " " + StatusResponse.LOGIN_FAILED.getCode() + " - Login failed");
						}
						break;
					case SET:
					case UNSET:
					case GET:
					case LOGOUT:
						statusResponse = StatusResponse.COMMAND_FORBIDDEN;
						logger.warn(logMessage + " " + StatusResponse.COMMAND_FORBIDDEN.getCode() + " - Unauthorize");
						break;
					default:
						statusResponse = StatusResponse.COMMAND_UNKNOWN;
						logger.error(logMessage + " " + StatusResponse.COMMAND_UNKNOWN.getCode() + " - Bad request");
						break;
					}
				} else {
					statusResponse = StatusResponse.COMMAND_UNKNOWN;
					logger.error(StatusResponse.COMMAND_UNKNOWN.getCode() + " - Bad request");
				}
				
			}
			return statusResponse.toString();
		} catch (Exception e) {
			logger.error(AdminCommand.GET.getMessage() + " " + StatusResponse.INTERNAL_ERROR.getCode() + " - " + StatusResponse.INTERNAL_ERROR.getMessage());
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
			logger.error("Error trying to close connection " + channel.getRemoteAddress());
			
		}
        buffer.clear();
	}
}