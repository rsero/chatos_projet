package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class PrivateConnectionClients {

	private Long connect_id = null;
	private final List<String> file;
	private SocketChannel sc;
	private final SocketChannel socketChannel;
	private ContextClient privateContext;
	private final ClientChatos clientChatos;
	 private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	
	public PrivateConnectionClients(ClientChatos clientChatos) throws IOException {
		this.file = new ArrayList<>();
		sc = SocketChannel.open();
		socketChannel = SocketChannel.open();
		this.clientChatos = clientChatos;
	}
	
	public PrivateConnectionClients(Long connect_id, ClientChatos clientChatos) throws IOException {
		this(clientChatos);
		this.connect_id = connect_id;
	}
	
	public void addConnectId(Long connect_id) {
		this.connect_id = connect_id;
	}
	
	public void addFileToSend(String newFile) {
		file.add(newFile);
	}
	
	public void removeFileToSend(String lastFile) {
		file.remove(lastFile);
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
}
