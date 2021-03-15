package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.server.reader.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Context {
    private static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);

    public Context(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
        this.sc = (SocketChannel) key.channel();
    }
    private void updateInterestOps() {
        int newInterestOps = 0;
        if (!closed && bbin.hasRemaining()) {
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }

        if (bbout.position() > 0) {
            newInterestOps = newInterestOps | SelectionKey.OP_WRITE;
        }
        if (newInterestOps == 0) {
            silentlyClose();
        }
        key.interestOps(newInterestOps);
    }

    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void doRead() throws IOException {
        if (sc.read(bbin) == -1) {
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

    private void processIn() {
//        for(;;) {
//            //Reader.ProcessStatus status = mr.process(bbin);
//            switch(status) {
//                case DONE:
//                    //Message msg = mr.get();
//                    //server.broadcast(msg);
//                    //mr.reset();
//                    break;
//                case REFILL:
//                    return;
//                case ERROR:
//                    silentlyClose();
//                    return;
//            }
//        }
    }

    private void processOut() {
//        while (!queue.isEmpty()) {
//            var msg = queue.remove();
//            bbout.putInt(UTF8.encode(msg.getLogin()).remaining());
//            bbout.put(UTF8.encode(msg.getLogin()));
//            bbout.putInt(UTF8.encode(msg.getMsg()).remaining());
//            bbout.put(UTF8.encode(msg.getMsg()));
//        }
    }


}
