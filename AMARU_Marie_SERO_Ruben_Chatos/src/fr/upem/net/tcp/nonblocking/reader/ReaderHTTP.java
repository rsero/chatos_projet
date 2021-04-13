package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface ReaderHTTP<Data> {

	ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException;

	Data get() throws IOException;

	void reset();
}
