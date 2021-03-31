package fr.upem.net.tcp.nonblocking.server.reader;

import java.nio.ByteBuffer;

public interface Reader<Data> {

    public static enum ProcessStatus {DONE,REFILL,ERROR};

    public ProcessStatus process(ByteBuffer bb);

    public Data get();

    public void reset();

}
