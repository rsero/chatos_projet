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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PrivateConnectionClients {

	private Long connect_id = null;
	private boolean connectionReady = false;
	private final List<String> files;
	private final String directory;
	private final SocketChannel socketChannel;
	private ContextPrivateClient privateContext;
	private final ClientChatos clientChatos;
	private final Charset charsetASCII = Charset.forName("ASCII");
	private static final Logger logger = Logger.getLogger(PrivateConnectionClients.class.getName());

	public PrivateConnectionClients(ClientChatos clientChatos, String directory) throws IOException {
		this.files = new ArrayList<>();
		socketChannel = SocketChannel.open();
		this.clientChatos = clientChatos;
		this.directory = directory;
	}

	public void launch(InetSocketAddress serverAddress, Selector selector) throws IOException {
		socketChannel.configureBlocking(false);
		var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		privateContext = new ContextPrivateClient(key, clientChatos);
		key.attach(privateContext);
		socketChannel.connect(serverAddress);
	}

	public long getConnectId(){
		return connect_id;
	}

	public void addConnectId(Long connect_id) {
		this.connect_id = connect_id;
	}

	public void addFileToSend(String newFile) {
		files.add(newFile);
	}

	public void removeFileToSend(String lastFile) {
		System.out.println("remove file "+files.size());
		files.remove(lastFile);
	}

	public boolean correctConnectId(Long id) {
		return id != null && id.equals(connect_id);
	}

	public void closeConnection(){
		privateContext.closeConnection();
	}

	public void queueMessage(ByteBuffer bb) {
		privateContext.queueMessage(bb);
	}

	public boolean activeConnection(SelectionKey key) {
		if(privateContext.equals(key.attachment())) {
			connectionReady = true;
			return true;
		}
		return false;
	}

	public boolean connectionReady() {
		return connectionReady;
	}

	public String getURL(String path) throws MalformedURLException {
		return new File(path).toURI().getPath();
	}

	public void sendRequest() {
		while(!files.isEmpty()) {
			var file = files.get(0);
			String request;
			try {
				request = "GET /"+ file + " HTTP/1.1\r\n"
						+ "Host: " + getURL(directory) + "\r\n"
						+ "\r\n";
				var bb = charsetASCII.encode(request);
				System.out.println("\n"+request);
				privateContext.queueMessage(bb);
			} catch (MalformedURLException e) {
				logger.warning(file + "doesn't exist");
			}
			removeFileToSend(file);
			//clientChatos.wakeUp();
		}
	}

	public boolean containsKey(SelectionKey key) {
		return privateContext != null && privateContext.equals(key.attachment());
	}
}
