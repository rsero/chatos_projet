package fr.upem.net.tcp.nonblocking.client;

import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.reader.HTTPReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class ContextPrivateClient implements Context {
    private final SelectionKey key;
    private final SocketChannel sc;
    private static int BUFFER_SIZE = 1024;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
    private HTTPReader httpReader = new HTTPReader();
    private boolean closed = false;
    private Object lock = new Object();
    private static final Logger logger = Logger.getLogger(ContextPrivateClient.class.getName());

    public ContextPrivateClient(SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
    }

    @Override
    public void processIn(ClientChatos client, SelectionKey key) throws IOException {
        for(;;) {
            ProcessStatus status = httpReader.process(bbin, key);
            switch (status) {
                case DONE:
                    Data value = httpReader.get();
                    value.decode(client, key);
                    httpReader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }
        }
    }

    @Override
    public void queueMessage(ByteBuffer bb) {
        queue.add(bb);
        processOut();
        updateInterestOps();
    }

    @Override
    public void processOut() {
        synchronized (lock) {
            while (!queue.isEmpty()) {
                var bb = queue.peek();
                if (bb.remaining() <= bbout.remaining()) {
                    queue.remove();
                    bbout.put(bb);
                }
            }
        }
    }

    @Override
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

    @Override
    public void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    @Override
    public void doRead(ClientChatos client, SelectionKey key) throws IOException {
        if (sc.read(bbin) == -1) {
            logger.info("read raté");
            closed = true;
        }
        processIn(client, key);
        updateInterestOps();
    }

    @Override
    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        processOut();
        updateInterestOps();
    }

    @Override
    public void doConnect() throws IOException {
        if (!sc.finishConnect())
            return; // the selector gave a bad hint
        key.interestOps(SelectionKey.OP_READ);
    }

    @Override
    public void closeConnection() {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

}
