package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Reader<Data> {

    public static enum ProcessStatus {DONE,REFILL,ERROR};

    public ProcessStatus process(ByteBuffer bb) throws IOException;

    public Data get() throws IOException;

    public void reset();

}
