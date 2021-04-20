package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
/**
 * Represents a reader that produces a String value
 */
public class StringReader implements Reader<String> {

	/**
	 * Different states the reader can be in
	 */
	private enum State {
		DONE, WAITING_INT, WAITING_STR, ERROR
	}

	private State state = State.WAITING_INT;
	private final ByteBuffer internalbb = ByteBuffer.allocate(1024); // write-mode
	private final IntReader intReader = new IntReader();
	private String msg;
	private int size;
	private final Charset UTF8 = StandardCharsets.UTF_8;

	/**
	 * Reads the ByteBuffer bb passed
	 * @param key
	 * @param bb
	 * @return ProcessStatus.REFILL if some content is missing, ProcessStatus.ERROR if an error
	 * occurred and ProcessStatus.DONE if all the content was processed
	 * @throws IllegalStateException if the state is DONE or ERROR at the beginning
	 */
	public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
		switch (state) {
			case WAITING_INT:
				var status = intReader.process(bb, key);
				switch (status) {
					case DONE:
						size = intReader.get();
						if (size < 0 || size > 1024) {
							return ProcessStatus.ERROR;
						}
						intReader.reset();
						state = State.WAITING_STR;
						internalbb.limit(size);
						break;
					case REFILL:
						return ProcessStatus.REFILL;
					case ERROR:
						state = State.ERROR;
						return ProcessStatus.ERROR;
				}
			case WAITING_STR:
				try {
					bb.flip();
					if (bb.remaining() <= internalbb.remaining()) {
						internalbb.put(bb);
					} else {
						var oldLimit = bb.limit();
						bb.limit(bb.position() + internalbb.remaining());
						internalbb.put(bb);
						bb.limit(oldLimit);
					}
					if (internalbb.hasRemaining()) {
						return ProcessStatus.REFILL;
					}
					state = State.DONE;
					internalbb.flip();
					msg = UTF8.decode(internalbb).toString();
					return ProcessStatus.DONE;
				} finally {
					bb.compact();
				}
			default:
				throw new IllegalStateException();

		}

	}

	/**
	 * Gets the String value that have been processed previously
	 * @return a String value
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public String get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return msg;
	}

	/**
	 * Resets the reader
	 */
	public void reset() {
		state = State.WAITING_INT;
		internalbb.clear();
		intReader.reset();
		msg = null;
	}
}

