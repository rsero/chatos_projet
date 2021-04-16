package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

public class ContextPublicClient implements Context {

	private final SelectionKey key;
	private final SocketChannel sc;
	private final ClientChatos client;
	private static int BUFFER_SIZE = 1024;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
	private InstructionReader reader = new InstructionReader();
	private boolean closed = false;
	private static final Logger logger = Logger.getLogger(ContextPublicClient.class.getName());
	private Object lock = new Object();
	private ClientDataTreatmentVisitor visitor;

	public ContextPublicClient(SelectionKey key, ClientChatos client) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.client=client;
		visitor = new ClientDataTreatmentVisitor(client);
	}

	/**
	 * Process the content of bbin
	 *
	 * The convention is that bbin is in write-mode before the call to process and
	 * after the call
	 * 
	 * @throws IOException
	 *
	 */
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

	/**
	 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
	 *
	 * @param bb
	 */
	public void queueMessage(ByteBuffer bb) {
		queue.add(bb);
		processOut();
		updateInterestOps();
	}

	/**
	 * Try to fill bbout from the message queue
	 *
	 */
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

	/**
	 * Update the interestOps of the key looking only at values of the boolean
	 * closed and of both ByteBuffers.
	 *
	 * The convention is that both buffers are in write-mode before the call to
	 * updateInterestOps and after the call. Also it is assumed that process has
	 * been be called just before updateInterestOps.
	 */

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

	/**
	 * Performs the read action on sc
	 *
	 * The convention is that both buffers are in write-mode before the call to
	 * doRead and after the call
	 *
	 * @throws IOException
	 */
	public void doRead() throws IOException {
		if (sc.read(bbin) == -1) {
			logger.info("read raté");
			closed = true;
		}
		processIn();
		updateInterestOps();
	}

	/**
	 * Performs the write action on sc
	 *
	 * The convention is that both buffers are in write-mode before the call to
	 * doWrite and after the call
	 *
	 * @throws IOException
	 */

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

	public void closeConnection(){
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	//@Override
	public void sendCommand(String msg) {
		//
	}
}