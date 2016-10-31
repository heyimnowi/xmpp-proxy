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

import ar.edu.itba.admin.StatusResponse;
import ar.edu.itba.config.ProxyConfiguration;
import ar.edu.itba.filters.SilentUser;
import ar.edu.itba.logger.XMPPProxyLogger;
import ar.edu.itba.utils.Utils;

public class AdminProxyHandler implements Handler {
	
	private InetSocketAddress listenAddress;
	private XMPPProxyLogger logger;
	private Selector selector;
	private ProxyConfiguration config;
	private final static int BUFFER_SIZE = 1024*100;
	private static String SUCCESSFULL_TYPE = "successfull";
	private static String ERROR_TYPE = "error";
	private static String ADMIN_INITIAL_MESSAGE = "</hello>\n";
	private static String ADMIN_FINAL_MESSAGE = "</bye>\n";
	private static String SILENT_USER_TAG = "^<silent-user>(.*)<\\/silent-user>\n$";
	private static String UNSILENT_USER_TAG = "^<silent-user>(.*)<\\/silent-user>\n$";
	private static String AUTH_TAG = "^<auth><user>(.*)<\\/user><password>(.*)<\\/password><\\/auth>\n$";
	private static String adminUser;
	private static String adminPassword;
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
		String response;
		
		AdminConnection connection = connections.get(channel);
		if (isHello(stringRead) && !connection.isHello()) {
			connection.setHello(true);
			writeInChannel(helloSuccess(), channel);
		} else {
			if (!connection.isLogged()) {
				if (isAuth(stringRead)) {
					response = authenticate(stringRead);
					if (response.equals(signInSuccess())) {
						connection.setLogged(true);
					}
					writeInChannel(response, channel);
				}
			} else {
				if (isSilent(stringRead)) {
					response = silentUser(stringRead);
					writeInChannel(response, channel);
					
				} else if (isUnsilent(stringRead)) {
					response = unsilentUser(stringRead);
					writeInChannel(response, channel);
				}
				else if (isBye(stringRead)) {
					//writeInChannel(response, channel);
				} else {
					response = badRequest();
					writeInChannel(response, channel);
				}
			}
		}
	}

	private String unsilentUser(String stringRead) {
		String user = Utils.regexRead(stringRead, SILENT_USER_TAG).group(1);
		String silenceProperty = config.getProperty("silenceuser");
		if (silenceProperty.equals("")) {
			config.setProperty("silenceuser", user);
		} else if (!silenceProperty.contains(user)) {
			config.setProperty("silenceuser", silenceProperty + "," + user);
		}
		SilentUser.getInstance().deleteUser(user);
		return unsilenceUserSuccess(user);
	}

	private String unsilenceUserSuccess(String user) {
		return StatusResponse.getStatus(SUCCESSFULL_TYPE, "200", "You have unsilent " + user);
	}

	private String silentUser(String stringRead) {
		String user = Utils.regexRead(stringRead, SILENT_USER_TAG).group(1);
		String silenceProperty = config.getProperty("silenceuser");
		if (silenceProperty.equals("")) {
			config.setProperty("silenceuser", user);
		} else if (!silenceProperty.contains(user)) {
			config.setProperty("silenceuser", silenceProperty + "," + user);
		}
		SilentUser.getInstance().addUser(user);
		return silenceUserSuccess(user);
	}

	private boolean isBye(String stringRead) {
		return Utils.regexMatch(stringRead, ADMIN_FINAL_MESSAGE);
	}

	private boolean isUnsilent(String stringRead) {
		return Utils.regexMatch(stringRead, UNSILENT_USER_TAG);
	}

	private boolean isSilent(String stringRead) {
		return Utils.regexMatch(stringRead, SILENT_USER_TAG);
	}

	private boolean isAuth(String stringRead) {
		return Utils.regexMatch(stringRead, AUTH_TAG);
	}

	private String authenticate(String stringRead) {
		Matcher matcher = Utils.regexRead(stringRead, AUTH_TAG);
		String user = matcher.group(1);
		String password = matcher.group(2);
		if (user.equals(adminUser) && password.equals(adminPassword)) {
			return signInSuccess();
		}
		return signInFail();
	}
	
	public boolean isHello(String stringRead) {
		return stringRead.equals(ADMIN_INITIAL_MESSAGE);
	}
		
	public void writeInChannel(String s, SocketChannel channel) throws IOException {
    	ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
			channel.write(buffer);
		} catch (IOException e) {
			logger.warn("Connection closed by client");
			
		}
        buffer.clear();
	}
	
	public static String helloSuccess() {
		return StatusResponse.getStatus(SUCCESSFULL_TYPE, "200", "Welcome to the nowixmppserver admin. Who are you?");
	}
	
	public String signInSuccess() {
		return StatusResponse.getStatus(SUCCESSFULL_TYPE, "200", "You’re the boss now! Start setting your configurations dude :)");
	}
	
	public String badRequest() {
		return StatusResponse.getStatus(ERROR_TYPE, "400", "Bad request bro");
	}
	
	public String signInFail() {
		return StatusResponse.getStatus(ERROR_TYPE, "401", "Mhhh please check your credentials");
	}
	
	public String silenceUserSuccess(String user) {
		return StatusResponse.getStatus(SUCCESSFULL_TYPE, "200", "You have silent " + user + ". You’re a bitch");
	}
	
	public static String signOutSuccess() {
		return StatusResponse.getStatus(SUCCESSFULL_TYPE, "200", "Hasta la vista, baby");
	}

	public ServerSocketChannel getChannel() {
		return adminChannel;
	}
}