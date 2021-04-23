package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

/**
 * Context of a public connection
 */
public class ContextPublicClient implements Context {

	private final static int BUFFER_SIZE = 1024;
	private final static Logger logger = Logger.getLogger(ContextPublicClient.class.getName());
	private final SelectionKey key;
	private final SocketChannel sc;
	private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	private final Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
	private final InstructionReader reader = new InstructionReader();
	private final ClientDataTreatmentVisitor visitor;
	private final Object lock = new Object();
	private boolean closed = false;

	public ContextPublicClient(SelectionKey key, ClientChatos client) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		visitor = new ClientDataTreatmentVisitor(client);
	}

	public void processIn() throws IOException {
		for (;;) {
			ProcessStatus status = reader.process(bbin, key);
			switch (status) {
			case DONE:
				Data value = reader.get();
				value.accept(visitor);
				reader.reset();
				break;
			case REFILL:
				return;
			case ERROR:
				silentlyClose();
				return;
			}
		}
	}

	public void queueMessage(ByteBuffer bb) {
		queue.add(bb);
		processOut();
		updateInterestOps();
	}

	public void processOut() {
		synchronized (lock){
			while (!queue.isEmpty()) {
				var bb = queue.peek();
				if (bb.remaining() <= bbout.remaining()) {
					queue.remove();
					bbout.put(bb);
				}
			}
		}
	}

	public void updateInterestOps() {
		var interestOps = 0;
		if (!closed && bbin.hasRemaining()) {
			interestOps = interestOps | SelectionKey.OP_READ;
		}
		if (!closed && bbout.position() != 0) {
			interestOps |= SelectionKey.OP_WRITE;
		}
		if (interestOps == 0) {
			silentlyClose();
			return;
		}
		key.interestOps(interestOps);
	}

	public void silentlyClose() {
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public void doRead() throws IOException {
		if (sc.read(bbin) == -1) {
			logger.info("read raté");
			closed = true;
		}
		processIn();
		updateInterestOps();
	}

	public void doWrite() throws IOException {
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	public void doConnect() throws IOException {
		if (!sc.finishConnect())
			return; // the selector gave a bad hint
		key.interestOps(SelectionKey.OP_READ);
	}
}