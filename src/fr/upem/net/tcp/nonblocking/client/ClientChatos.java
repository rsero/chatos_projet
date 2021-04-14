package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.data.*;

public class ClientChatos {

	private static final int BUFFER_SIZE = 1024;
	private static final Logger logger = Logger.getLogger(ClientChatos.class.getName());
	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private final String directory;
	private Login login;
	private String loginAsked;
	private final Thread console;
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private final HashMap<Login, PrivateRequest> hashPrivateRequest = new HashMap<>();// Client qui demande la connexion
																						// privée
	private final HashMap<Login, ContextPrivateClient> privateContexts = new HashMap<>();// Client connecté a l'aide
																							// d'une connexion privée
	private ContextPublicClient uniqueContext;
	private final Object lock = new Object();
	private final Thread privateConnectionThread;

	public ClientChatos(InetSocketAddress serverAddress, String directory) throws IOException {
		this.serverAddress = serverAddress;
		this.directory = directory;
		this.login = new Login();
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
		this.privateConnectionThread = new Thread(this::privateConnection);
	}

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

	private void privateConnection() {
		while (!Thread.interrupted()) {
			synchronized (lock) {
				for (var login : privateContexts.keySet()) {
					var files = privateContexts.get(login).getFiles(login);
					if(files!= null)
						privateContexts.get(login).sendCommand(login);
				}
				selector.wakeup();
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

	public void addConnect_id(Long connectId, Login loginTarget) throws IOException {
		privateContexts.get(loginTarget).setConnect_id(connectId);
	}

	public Optional<ByteBuffer> parseInput(String input) throws IOException {
		Optional<ByteBuffer> bb;
		synchronized (lock) {
			var req = ByteBuffer.allocate(BUFFER_SIZE);
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
					bb = parsePrivateMessage(req, content, data);
					break;
				case '/': // connexion privée
					bb = parsePrivateConnection(req, content, data, elements);
					break;
				default: // message global
					bb = parseMessageGlobal(req, input);
					break;
				}
			}
		}
		return bb;
	}

	private Optional<ByteBuffer> parsePrivateMessage(ByteBuffer req, String content, String data) throws IOException {
		var msgprive = new PrivateMessage(login, new Login(content), data);
		return Optional.of(msgprive.encode(req));
	}

	private Optional<ByteBuffer> parsePrivateConnection(ByteBuffer req, String content, String data, String[] elements)
			throws IOException {
		if (content.equals("y") && hashPrivateRequest.containsKey(new Login(data))) {
			return parseAcceptPrivateConnection(req, data);
		} else if (content.equals("n") && hashPrivateRequest.containsKey(new Login(data))) {
			return parseRefusePrivateConnection(req, data);
		} else if (content.equals("y") || content.equals("n")) {// Accepte une connection privée d'un client qui ne l'a
																// pas demandé
			System.out.println("This client doesn't ask the connexion");
			return Optional.empty();
		} else if (privateContexts.containsKey(new Login(content)) && data.isEmpty()){
			return disconnectPrivateClient(req, new Login(content));
		} else if (content.equals("id")) {
			return parseConnectId(req, data);
		} else {
			return parseRequestPrivateConnection(req, data, content, elements);
		}
	}

	private Optional<ByteBuffer> parseRequestPrivateConnection(ByteBuffer req, String data, String content,
			String[] elements) throws IOException {
		if (data.isEmpty()) {
			System.out.println("Usage : /login file");
			return Optional.empty();
		}
		var targetLogin = new Login(content);
		if(targetLogin.equals(login)){
			System.out.println("This is your username");
			return Optional.empty();
		}
		var privateRequest = new PrivateRequest(login, targetLogin);
		var file = elements[1];
		if(privateContexts.putIfAbsent(targetLogin, new ContextPrivateClient(selector, directory, serverAddress, 0))==null){
			System.out.println("targetlogin after /y login is "+ targetLogin);
			privateContexts.get(targetLogin).addFileToMap(targetLogin,file);
			return Optional.of(privateRequest.encodeAskPrivateRequest(req));
		}
		return Optional.empty();
	}

	private Optional<ByteBuffer> parseConnectId(ByteBuffer req, String data) throws IOException {
		Optional<ByteBuffer> bb;
		if (data.isEmpty()) {
			System.out.println("Usage : /id connect_id");
			return Optional.empty();
		}
		try {
			Long connect_id = Long.valueOf(data);
			var correctId = false;
			synchronized (lock) {
				for (var value : privateContexts.values()) {
					if (value.correctConnectId(connect_id)) {
						correctId = true;
						var privateLogin = new PrivateLogin(connect_id);
						bb = Optional.of(privateLogin.encode(req));
						if (!bb.isPresent()) {
							break;
						}
						value.queueMessage(bb.get());
					}
				}
			}
			if (!correctId) {
				System.out.println("Incorrect connect_id");
				return Optional.empty();
			}
			return Optional.empty();// On remets a vide parce que le context principal n'envoie rien
		} catch (NumberFormatException nb) {
			System.out.println("Usage : /id connect_id");
			return Optional.empty();
		}
	}

	private Optional<ByteBuffer> parseRefusePrivateConnection(ByteBuffer req, String data) {
		if (data.isEmpty()) {
			System.out.println("Usage : /n login");
			return Optional.empty();
		}
		var loginToRemove = new Login(data);
		var privateRequest = hashPrivateRequest.remove(loginToRemove);
		privateContexts.remove(loginToRemove);
		System.out.println("Private connection refused");
		return Optional.of(privateRequest.encodeRefusePrivateRequest(req));
	}

	private Optional<ByteBuffer> parseAcceptPrivateConnection(ByteBuffer req, String data){
		if (data.isEmpty()) {
			System.out.println("Usage : /y login");
			return Optional.empty();
		}
		var privateRequest = hashPrivateRequest.get(new Login(data));
		System.out.println("Private connection with " + data + " accepted");
		return Optional.of(privateRequest.encodeAcceptPrivateRequest(req));
	}

	private Optional<ByteBuffer> parseMessageGlobal(ByteBuffer req, String input) throws IOException {
		var messageGlobal = new MessageGlobal(login, input);
		return Optional.of(messageGlobal.encode(req));
	}

	private Optional<ByteBuffer> disconnectPrivateClient(ByteBuffer req, Login loginTarget) throws IOException {
		synchronized (lock) {
			var connectId = privateContexts.get(loginTarget).getConnectId();
			privateContexts.get(loginTarget).closeConnection();
			privateContexts.remove(loginTarget);
			var disconnectRequest = new DisconnectRequest(connectId, login, loginTarget);
			return Optional.of(disconnectRequest.encode(req));
		}
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		uniqueContext = new ContextPublicClient(key);
		key.attach(uniqueContext);
		sc.connect(serverAddress);
		console.start();
		privateConnectionThread.start();
		while (!Thread.interrupted()) {
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				((Context) key.attachment()).doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();

			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead(this, key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		new ClientChatos(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2]).launch();
	}

	private static void usage() {
		System.out.println("Usage : ClientChat hostname port directory");
	}

	public void updateLogin() {
		login = new Login(loginAsked);
	}

	public boolean isConnected() {
		return !login.isNotConnected();
	}

	public Login getLogin() {
		return login;
	}

	public void addSetPrivateRequest(PrivateRequest privateRequest) {
		synchronized (lock) {
			hashPrivateRequest.put(privateRequest.getLoginRequester(), privateRequest);
		}
	}
/*
	public void activePrivateConnection(SelectionKey key) {
		synchronized (lock) {
			for (var privateClient : hashLoginFile.values()) {
				if (privateClient.activeConnection(key)) {
					return;
				}
			}
		}
	}



	public boolean isConnectionPrivate(SelectionKey key) {
		synchronized (lock) {
			for (var privateClient : hashLoginFile.values()) {
				if (privateClient.containsKey(key)) {
					if (privateClient.connectionReady())
						return true;
				}
			}
		}
		return false;
	}
*/
	public String getDirectory(){
		return directory;
	}

	public void deleteRequestConnection(Login loginTarget) {
		synchronized (lock) {
			privateContexts.remove(loginTarget);
		}
	}

	public void addConnection(Login login) throws IOException {
		privateContexts.putIfAbsent(login, new ContextPrivateClient(selector, directory, serverAddress, 0));
	}
}
