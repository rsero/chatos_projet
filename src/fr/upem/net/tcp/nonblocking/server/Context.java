package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class Context {
    private static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<Data> queue = new LinkedList<>();
    private InstructionReader reader = new InstructionReader();
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
//            return;
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
        	Reader.ProcessStatus status = reader.process(bbin);
            switch (status) {
                case DONE:
                    var data = (Data) reader.get();
                    server.broadcast(data);
                    reader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }
    }

    private void processOut() {
        while (!queue.isEmpty()) {
            var data = queue.peek();
            if(data.processOut(bbout, this, server)) {
            	queue.remove();
            }
        }
    }

    public void queueMessage(Data data) {
        queue.add(data);
        processOut();
        updateInterestOps();
    }
}
