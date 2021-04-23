package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.data.*;

/**
 * Represents a client which will be able to connect to the server or to another client through a private connection
 * Reads the input from the user through the console
 */
public class ClientChatos {

	/**
	 * Maximum buffer capacity
	 */
	private static final int BUFFER_SIZE = 1024;
	/**
	 * Logger of the class ClientChatos
	 */
	private static final Logger logger = Logger.getLogger(ClientChatos.class.getName());
	/**
	 * SocketChannel on which the connection is established
	 */
	private final SocketChannel sc;
	/**
	 * Selector for the client
	 */
	private final Selector selector;
	/**
	 * Address of the server the client wants to connect to
	 */
	private final InetSocketAddress serverAddress;
	/**
	 * Name of the directory the client will receive files on
	 */
	private final String directory;
	/**
	 * Unique login the client is authenticated on the server with
	 */
	private Login login;
	/**
	 * String value to associate to the previous Login object
	 */
	private String loginAsked;
	/**
	 * Thread on which the console runs and reads the input from the client
	 */
	private final Thread console;
	/**
	 * Queue which contains data passed by the client to be sent
	 */
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	/**
	 * Map of the clients asking for a private connection
	 */
	private final HashMap<Login, PrivateRequest> privateConnectionsRequests = new HashMap<>();
	/**
	 * Map of the clients connected to the client with a private connection
	 */
	private final HashMap<Login, PrivateConnectionClients> privateConnectionsEstablished = new HashMap<>();
	/**
	 * Context associated with the client
	 */
	private ContextPublicClient uniqueContext;
	/**
	 * Object to synchronize on
	 */
	private final Object lock = new Object();

	/**
	 * Constructor for a ClientChatos object
	 * @param serverAddress address to connect on
	 * @param directory name of the directory to save file on
	 * @throws IOException
	 */
	public ClientChatos(InetSocketAddress serverAddress, String directory) throws IOException {
		this.serverAddress = serverAddress;
		this.directory = directory;
		this.login = new Login();
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}

	/**
	 * Action the console Thread does to read input
	 */
	private void consoleRun() {
		try (var scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				var line = scan.nextLine();
				sendCommand(line);
			}
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	/**
	 * Iterates through the clients connected with a private connection and
	 * checks if any files are waiting to be sent
	 */
	public void privateConnection() {
			synchronized (lock) {
				for (var privateConnection : privateConnectionsEstablished.values()) {
					if (privateConnection.connectionReady()) {
						privateConnection.sendRequest();
					}
				}
			}
	}

	/**
	 * Send a command to the selector via commandQueue and wake it up
	 *
	 * @param msg Message
	 * @throws InterruptedException
	 */
	private void sendCommand(String msg) throws InterruptedException {
		synchronized (lock) {
			commandQueue.add(msg);
			wakeUp();
		}
	}

	/**
	 * Wakes up the selector
	 */
	public void wakeUp(){
		selector.wakeup();
	}

	/**
	 * Processes the command from commandQueue
	 * 
	 * @throws IOException
	 */
	private void processCommands() throws IOException {
		synchronized (commandQueue) {
			while (!commandQueue.isEmpty()) {
				var command = commandQueue.remove();
				Optional<ByteBuffer> bb = parseInput(command);
				if (!bb.isPresent()) {
					return;
				}
				uniqueContext.queueMessage(bb.get().flip());
			}
		}
	}

	/**
	 * Adds a new client to the map if a private connection is established, and launches the connection
	 * @param connectId the connectid corresponding to the private connection
	 * @param loginTarget the login of the other client to establish the private connection with
	 * @throws IOException
	 */
	public void addConnect_id(Long connectId, Login loginTarget) throws IOException {
		synchronized (lock) {
			privateConnectionsEstablished.putIfAbsent(loginTarget, new PrivateConnectionClients(this, directory));
			privateConnectionsEstablished.get(loginTarget).addConnectId(connectId);
			privateConnectionsEstablished.get(loginTarget).launch(serverAddress, selector);
		}
	}

	/**
	 * Parses the user input :
	 * - prints indications if the input is empty or if the login contains any space
	 * - analyses the first character of the input, and calls the accurate parsing method
	 * according to the content passed
	 * @param input the string value passed in the console by the client
	 * @return an Optional value of the data encoded in a buffer if everything worked,
	 * or an Optional empty if something went wrong
	 * @throws IOException
	 */
	public Optional<ByteBuffer> parseInput(String input) throws IOException {
		Optional<ByteBuffer> bb;
		synchronized (lock) {
			if (input.isEmpty() || input.startsWith(" ")) {
				System.out.println("Usage : no empty messages");
				return Optional.empty();
			}
			if (login.isNotConnected()) {
				if(!input.contains(" ")) {
					loginAsked = input;
					bb = Optional.of(login.encodeLogin(input));
				} else {
					System.out.println("There cannot be any space in your username");
					bb = Optional.empty();
				}
			} else {
				var elements = input.split(" ", 2);
				var prefix = elements[0].charAt(0);
				var content = elements[0].substring(1);
				var data = elements.length == 1 ? "" : elements[1];
				switch (prefix) {
				case '@': // message privé
					bb = parsePrivateMessage(content, data);
					break;
				case '/': // connexion privée
					bb = parsePrivateConnection(content, data);
					break;
				default: // message global
					bb = parseMessageGlobal(input);
					break;
				}
			}
		}
		return bb;
	}

	/**
	 * Parses the content of a private message, and creates a new PrivateMessage object
	 * with the informations parsed.
	 * Encodes the PrivateMessage object.
	 * @param content the login of the target
	 * @param data the content of the message
	 * @return an optional value of the encoded private message in a buffer
	 */
	private Optional<ByteBuffer> parsePrivateMessage(String content, String data) {
		var msgprive = new PrivateMessage(login, new Login(content), data);
		return Optional.of(msgprive.encode());
	}

	/**
	 * Parses the content related to a private connection :
	 * - if the input starts with "\y" and this client has requested a private connection before, calls the accept private connection parsing method
	 * - if the input starts with "\n" and this client has requested a private connection before, calls the refuse private connection parsing method
	 * - if the input starts with "\y" or "\n" but the login passed has not requested a private connection, returns an empty value.
	 * - if the input is "\login" and this client is connected already, calls the disconnect private connection method.
	 * - if the input starts with "\id", calls the connect id parsing method
	 * - else calls the request private connection parsing method
	 * @param content the string value located after the "\" character
	 * @param data the string value located after the first space of the input
	 * @return an optional value of the encoded private connection object in a buffer
	 * @throws IOException
	 */
	private Optional<ByteBuffer> parsePrivateConnection(String content, String data)
			throws IOException {
		if (content.equals("y") && privateConnectionsRequests.containsKey(new Login(data))) {
			return parseAcceptPrivateConnection(data);
		} else if (content.equals("n") && privateConnectionsRequests.containsKey(new Login(data))) {
			return parseRefusePrivateConnection(data);
		} else if (content.equals("y") || content.equals("n")) {
			System.out.println("This client doesn't ask the connexion");
			return Optional.empty();
		} else if (data.isEmpty() && privateConnectionsEstablished.containsKey(new Login(content))){
			System.out.println("Disconnection is asked");
			return disconnectPrivateClient(new Login(content));
		} else if (content.equals("id")) {
			return parseConnectId(data);
		} else {
			return parseRequestPrivateConnection(data, content);
		}
	}

	/**
	 * Parses the content related to the request private connection :
	 * - if the name of the file requested is empty, notifies that there was a mistake
	 * - if the login passed is your login, notifies that there was a mistake
	 * - else creates a PrivateRequest object with your login and the login requested, adds it
	 * to the map of established private connections and adds the file to the list of files to send
	 * - calls the private Connection method
	 * @param file the name of the file requested
	 * @param login the login of the target client
	 * @return an optional value of the encoded private request in a buffer
	 * @throws IOException
	 */
	private Optional<ByteBuffer> parseRequestPrivateConnection(String file, String login) throws IOException {
		if (file.isEmpty()) {
			System.out.println("Usage : /login file");
			return Optional.empty();
		}
		var targetLogin = new Login(login);
		if(targetLogin.equals(login)){
			System.out.println("This is your username");
			return Optional.empty();
		}
		var privateRequest = new PrivateRequest(this.login, targetLogin);
		if (privateConnectionsEstablished.putIfAbsent(targetLogin, new PrivateConnectionClients(this, directory)) == null) {
			privateConnectionsEstablished.get(targetLogin).addFileToRequest(file);
			return Optional.of(privateRequest.encodeAskPrivateRequest());
		}
		privateConnectionsEstablished.get(targetLogin).addFileToRequest(file);
		privateConnection();
		return Optional.empty();
	}

	/**
	 * Parses the content related to the request private connection :
	 * - if the connectid value is empty or not a long value, notifies the user
	 * - if the connectid corresponds to one of the client in the map of the established private connections
	 * adds a privateLogin object to the queue of the private connection
	 * - if the connectid is not found in the list of client, returns an empty value
	 * @param connectId string value corresponding to the connectid
	 * @return an empty value because the nothing is sent on the public connection
	 */
	private Optional<ByteBuffer> parseConnectId(String connectId) {
		Optional<ByteBuffer> bb;
		if (connectId.isEmpty()) {
			System.out.println("Usage : /id connect_id");
			return Optional.empty();
		}
		try {
			Long connect_id = Long.valueOf(connectId);
			var correctId = false;
			synchronized (lock) {
				for (var value : privateConnectionsEstablished.values()) {
					if (value.correctConnectId(connect_id)) {
						correctId = true;
						var privateLogin = new PrivateLogin(connect_id);
						bb = Optional.of(privateLogin.encode());
						value.queueMessage(bb.get().flip());
					}
				}
			}
			if (!correctId) {
				System.out.println("Incorrect connect_id");
				return Optional.empty();
			}
			return Optional.empty();
		} catch (NumberFormatException nb) {
			System.out.println("Usage : /id connect_id");
			return Optional.empty();
		}
	}

	/**
	 * Parses the content related to the refusal of a private connection :
	 * - if the login passed is empty, notifies the user
	 * - else removes the login of the client from the map of private connection requests
	 * and encodes a privateRequest object with a refusing opCode to send back
	 * @param login login of the client to refuse
	 * @return an optional value of the encoded private request in a buffer
	 */
	private Optional<ByteBuffer> parseRefusePrivateConnection(String login) {
		if (login.isEmpty()) {
			System.out.println("Usage : /n login");
			return Optional.empty();
		}
		var loginToRemove = new Login(login);
		var privateRequest = privateConnectionsRequests.remove(loginToRemove);
		System.out.println("Private connection refused");
		return Optional.of(privateRequest.encodeRefusePrivateRequest());
	}

	/**
	 * Parses the content related to the acceptation of a private connection :
	 * - if the login passed is empty, notifies the user
	 * - encodes a privateRequest object with an accepting opCode to send back
	 * @param login
	 * @return an optional value of the encoded private request in a buffer
	 */
	private Optional<ByteBuffer> parseAcceptPrivateConnection(String login){
		if (login.isEmpty()) {
			System.out.println("Usage : /y login");
			return Optional.empty();
		}
		var privateRequest = privateConnectionsRequests.get(new Login(login));
		System.out.println("Private connection with " + login + " accepted");
		return Optional.of(privateRequest.encodeAcceptPrivateRequest());
	}

	/**
	 * Parses the content of a global message, and creates a new MessageGlobal object
	 * with the content parsed.
	 * Encodes the PrivateMessage object.
	 * @param input the content of the message
	 * @return an optional value of the encoded public message in a buffer
	 */
	private Optional<ByteBuffer> parseMessageGlobal(String input) {
		var messageGlobal = new MessageGlobal(login, input);
		return Optional.of(messageGlobal.encode());
	}

	/**
	 * Disconnects a private connection by removing the connection from the established private connections map
	 * and closing the connection.
	 * @param loginTarget login of the client to disconnect from
	 * @return an optional value of the disconnect request in a buffer
	 */
	private Optional<ByteBuffer> disconnectPrivateClient(Login loginTarget) {
		synchronized (lock) {
			var connectId = privateConnectionsEstablished.get(loginTarget).getConnectId();
			privateConnectionsEstablished.get(loginTarget).closeConnection();
			privateConnectionsEstablished.remove(loginTarget);
			var disconnectRequest = new DisconnectRequest(connectId, login, loginTarget);
			return Optional.of(disconnectRequest.encode());
		}
	}

	/**
	 * Launches the client by configuring its socket channel, creating a public context and starting the console Thread
	 * @throws IOException if an error occured
	 */
	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		uniqueContext = new ContextPublicClient(key, this);
		key.attach(uniqueContext);
		sc.connect(serverAddress);
		console.start();
		while (!Thread.interrupted()) {
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	/**
	 * Treats the Selection key
	 * @param key the Selection key to treat
	 */
	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				((Context) key.attachment()).doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}

	/**
	 * Main method creating a ClientChatos object and launches it.
	 * @param args the address to connect to, the host and the directory to save files on
	 * @throws NumberFormatException if the port number is not an Integer
	 * @throws IOException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		new ClientChatos(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2]).launch();
	}

	/**
	 * Output if mistake in the arguments passed when creating a client
	 */
	private static void usage() {
		System.out.println("Usage : ClientChat hostname port directory");
	}

	/**
	 * Assigns a Login object to the client
	 */
	public void updateLogin() {
		login = new Login(loginAsked);
	}

	/**
	 * Checks if the client is authenticated or not yet
	 * @return true if the client has a Login
	 */
	public boolean isConnected() {
		return !login.isNotConnected();
	}

	/**
	 * Returns the login of the client
	 */
	public Login getLogin() {
		return login;
	}

	/**
	 * Adds a privateRequest object to the map of private connections requests
	 * @param privateRequest privateRequest to add to the map
	 */
	public void addSetPrivateRequest(PrivateRequest privateRequest) {
		synchronized (lock) {
			privateConnectionsRequests.put(privateRequest.getLoginRequester(), privateRequest);
		}
	}

	/**
	 * Activates a private connection
	 * @param key key of the connection to activate
	 */
	public void activePrivateConnection(SelectionKey key) {
		synchronized (lock) {
			for (var privateClient : privateConnectionsEstablished.values()) {
				if (privateClient.activeConnection(key)) {
					return;
				}
			}
		}
	}

	/**
	 * Gets the directory of the client
	 * @return the directory to save files on
	 */
	public String getDirectory(){
		return directory;
	}

	/**
	 * Removes the login passed from the map of established private connections
	 * @param loginTarget login of the client to remove from the map
	 */
	public void deleteRequestConnection(Login loginTarget) {
		synchronized (lock) {
			privateConnectionsEstablished.remove(loginTarget);
		}
	}
}
