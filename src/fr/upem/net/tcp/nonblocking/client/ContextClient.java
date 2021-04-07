package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.reader.HTTPRequestReader;
import fr.upem.net.tcp.nonblocking.server.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;

public class ContextClient {

	final private SelectionKey key;
	final private SocketChannel sc;
	static private int BUFFER_SIZE = 1024;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
	private InstructionReader reader = new InstructionReader();
	private HTTPRequestReader httpReader = new HTTPRequestReader();
	private boolean closed = false;

	public ContextClient(SelectionKey key) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
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
	private void processIn(ClientChatos client, SelectionKey key) throws IOException {
		System.out.println("processIn");
		
		for (var cpt =0 ; ; cpt++) {
			boolean isPrivateConnection = client.isConnectionPrivate(key);
			Reader.ProcessStatus status;
			if (isPrivateConnection && cpt == 0) {
				status = httpReader.process(bbin);
			} else {
				status = reader.process(bbin);
			}
			switch (status) {
			case DONE:
				if (isPrivateConnection && cpt == 0) {
					Data value = httpReader.get();
					value.decode(client, key);
					httpReader.reset();
				} else {
					Data value = reader.get();
					value.decode(client, key);
					reader.reset();
				}
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
	private void processOut() {
		while (!queue.isEmpty()) {
			var bb = queue.peek();
			if (bb.remaining() <= bbout.remaining()) {
				queue.remove();
				bbout.put(bb);
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

	private void updateInterestOps() {
		var interesOps = 0;
		if (!closed && bbin.hasRemaining()) {
			interesOps = interesOps | SelectionKey.OP_READ;
		}
		if (!closed && bbout.position() != 0) {
			interesOps |= SelectionKey.OP_WRITE;
		}
		if (interesOps == 0) {
			silentlyClose();
			return;
		}
		key.interestOps(interesOps);
	}

	private void silentlyClose() {
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
	public void doRead(ClientChatos client, SelectionKey key) throws IOException {
		if (sc.read(bbin) == -1) {
			System.out.println("read raté");
			closed = true;
		}
		processIn(client, key);
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
		System.out.println("J'espedie le paquet");
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
		System.out.println("j'au expedié le paquet");
	}

	public void doConnect() throws IOException {
		if (!sc.finishConnect())
			return; // the selector gave a bad hint
		key.interestOps(SelectionKey.OP_READ);
	}
}