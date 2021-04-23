package fr.upem.net.tcp.nonblocking.client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a private connection and its elements
 */
public class PrivateConnectionClients {

	/**
	 * Long value corresponding to private connection
	 */
	private Long connect_id = null;
	/**
	 * Boolean value indicating if the connection can be established
	 */
	private boolean connectionReady = false;
	/**
	 * List of the files requested during the private connection
	 */
	private final List<String> files;
	/**
	 * Directory where the file can be saved
	 */
	private final String directory;
	/**
	 * New socket channel to establish the private connection on
	 */
	private final SocketChannel socketChannel;
	/**
	 * Private Context associated to the private connection
	 */
	private ContextPrivateClient privateContext;
	/**
	 * Main client
	 */
	private final ClientChatos clientChatos;
	/**
	 * Charset to encode http headers with
	 */
	private final Charset charsetASCII = StandardCharsets.US_ASCII;
	/**
	 * Logger of the PrivateConnectionClients class
	 */
	private static final Logger logger = Logger.getLogger(PrivateConnectionClients.class.getName());

	/**
	 * Constructor for the PrivateConnectionClients class
	 * @param clientChatos main client
	 * @param directory directory to save files on
	 * @throws IOException if the connection is closed
	 */
	public PrivateConnectionClients(ClientChatos clientChatos, String directory) throws IOException {
		this.files = new ArrayList<>();
		socketChannel = SocketChannel.open();
		this.clientChatos = clientChatos;
		this.directory = directory;
	}

	/**
	 * Launches the private connection by connecting it to the server address, and attaches a private context to it
	 * @param serverAddress to connect to
	 * @param selector same selector as the main client
	 * @throws IOException is the connection is lost
	 */
	public void launch(InetSocketAddress serverAddress, Selector selector) throws IOException {
		socketChannel.configureBlocking(false);
		var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		privateContext = new ContextPrivateClient(key, clientChatos);
		key.attach(privateContext);
		socketChannel.connect(serverAddress);
	}

	/**
	 * Returns the connect id of the private connection
	 * @return long value of the connect id
	 */
	public long getConnectId(){
		return connect_id;
	}

	/**
	 * Updates the connect id of the private connection
	 * @param connect_id value to assign as the connect id
	 */
	public void addConnectId(Long connect_id) {
		this.connect_id = connect_id;
	}

	/**
	 * Adds the name of a file to the list of files to request
	 * @param newFile file to add
	 */
	public void addFileToRequest(String newFile) {
		files.add(newFile);
	}

	/**
	 * Removes the name of the file from the list of files to request
	 * @param lastFile file to remove
	 */
	public void removeFileToRequest(String lastFile) {
		files.remove(lastFile);
	}

	/**
	 * Checks is the connect id correspond to the current private connection
	 * @param id connect id to compare
	 * @return true if the connect ids are the same, else false
	 */
	public boolean correctConnectId(Long id) {
		return id != null && id.equals(connect_id);
	}

	/**
	 * Closes the connection on the private context
	 */
	public void closeConnection(){
		privateContext.silentlyClose();
	}

	/**
	 * Add a buffer to the message queue of the private context
	 *
	 * @param bb buffer to add
	 */
	public void queueMessage(ByteBuffer bb) {
		privateContext.queueMessage(bb);
	}

	/**
	 * Verifies if the key passed corresponds to the key of the private context, and if so puts the connectionReady boolean to true
	 * @param key key to compare with the private context
	 * @return true if the connection is ready, else false
	 */
	public boolean activeConnection(SelectionKey key) {
		if(privateContext.equals(key.attachment())) {
			connectionReady = true;
			return true;
		}
		return false;
	}

	/**
	 * Returns the value of the boolean connectionReady
	 * @return connectionReady boolean
	 */
	public boolean connectionReady() {
		return connectionReady;
	}

	/**
	 * Creates the URL from the path of the directory to save the file
	 * @param path path of the directory to save the file on
	 * @return the URL of the directory
	 * @throws MalformedURLException if the path is not in the right format
	 */
	public String getURL(String path) throws MalformedURLException {
		return new File(path).toURI().getPath();
	}

	/**
	 * Iterates through the list of files to request and add an encoded GET request for each to the queue of the private context
	 * Finally removes the file from the list
	 */
	public void sendRequest() {
		while(!files.isEmpty()) {
			var file = files.get(0);
			String request;
			try {
				request = "GET /"+ file + " HTTP/1.1\r\n"
						+ "Host: " + getURL(directory) + "\r\n"
						+ "\r\n";
				var bb = charsetASCII.encode(request);
				privateContext.queueMessage(bb);
			} catch (MalformedURLException e) {
				logger.warning(file + "doesn't exist");
			}
			removeFileToRequest(file);
		}
	}
}
