package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Reader<Data> {

	ProcessStatus process(ByteBuffer bb) throws IOException;

    Data get() throws IOException;

    void reset();

}
