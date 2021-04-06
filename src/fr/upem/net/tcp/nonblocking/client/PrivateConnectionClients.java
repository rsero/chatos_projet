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

public class PrivateConnectionClients {

	private Long connect_id = null;
	private boolean connectionReady = false;
	private final List<String> files;
	//private SocketChannel sc;
	private final String directory;
	private final SocketChannel socketChannel;
	private ContextClient privateContext;
	private final ClientChatos clientChatos;
	private final Charset charsetASCII = Charset.forName("ASCII");
	//private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	
	public PrivateConnectionClients(ClientChatos clientChatos, String directory) throws IOException {
		this.files = new ArrayList<>();
		//sc = SocketChannel.open();
		socketChannel = SocketChannel.open();
		this.clientChatos = clientChatos;
		this.directory = directory;
	}
	
//	public PrivateConnectionClients(Long connect_id, ClientChatos clientChatos, String directory) throws IOException {
//		this(clientChatos, directory);
//		this.connect_id = connect_id;
//	}
	
	public void addConnectId(Long connect_id) {
		this.connect_id = connect_id;
	}
	
	public void addFileToSend(String newFile) {
		files.add(newFile);
	}
	
	public void removeFileToSend(String lastFile) {
		files.remove(lastFile);
	}
	
	public boolean correctConnectId(Long id) {
		return id != null && id.equals(connect_id);
	}
		
	public void launch( InetSocketAddress serverAddress, Selector selector) throws IOException {
		socketChannel.configureBlocking(false);
        var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        privateContext = new ContextClient(key);
        key.attach(privateContext);
        socketChannel.connect(serverAddress);   
    }

	public void queueMessage(Optional<ByteBuffer> bb) {
		privateContext.queueMessage(bb.get().flip());
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
        return new File(path).toURI().toURL().toString();
    }

	public void sendRequest() {
		while(!files.isEmpty()) {
			var file = files.get(0);
			String request;
			try {
				request = "GET / HTTP/1.1\r\n"
				        + "Host: " + getURL(directory + "/" + file) + "\r\n"
				        + "\r\n";
				System.out.println(request);
				var bb = Optional.of(charsetASCII.encode(request));
				queueMessage(bb);
			} catch (MalformedURLException e) {
				System.out.println(file + "doesn't exist");
			}
			removeFileToSend(file);
		}
	}

	public boolean containsKey(SelectionKey key) {	
		return privateContext != null && privateContext.equals(key.attachment());
	}
}
