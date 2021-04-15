package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ContextPrivateServer implements Context {
    private final static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<ByteBuffer> queue = new LinkedList<>();
    private final PrivateConnexionTransmissionReader privateConnexionTransmissionReader;
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Object lock = new Object();

    public ContextPrivateServer(ServerChatos server, SelectionKey key, SocketChannel sc){
        this.server=server;
        this.key=key;
        this.sc = sc;
        privateConnexionTransmissionReader = new PrivateConnexionTransmissionReader(key);
    }

    @Override
    public void processIn() throws IOException {
        System.out.println("processin hors du for");
        for (;;) {
            System.out.println("processin dans le for");
            ProcessStatus status;
            privateConnexionTransmissionReader.reset();
            status = privateConnexionTransmissionReader.process(bbin, key);
            switch (status) {
                case DONE:
                    Data data = (Data) privateConnexionTransmissionReader.get();
                    privateConnexionTransmissionReader.reset();
                    server.broadcast(data, this);
                    return;
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
        synchronized (lock){
            queue.add(bb);
            processOut();
            updateInterestOps();
        }
    }

    @Override
    public void processOut() {
        synchronized (lock){
            while (!queue.isEmpty()) {
                var data = queue.peek();
                if(data.remaining() <= bbout.remaining()){
                    bbout.put(data);
                    queue.remove();
                }
            }
        }
    }

    @Override
    public void updateInterestOps() {
        int newInterestOps = 0;
        if (!closed && bbin.hasRemaining()) {
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }
        if (!closed && bbout.position() > 0) {
            newInterestOps = newInterestOps | SelectionKey.OP_WRITE;
        }
        if (newInterestOps == 0) {
            server.close(this);
            silentlyClose();
            return;
        }
        key.interestOps(newInterestOps);
    }

    @Override
    public void silentlyClose() {
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

    @Override
    public void doConnect() throws IOException {
        // do nothing
    }

    @Override
    public void closeConnection() {
        //do nothing
    }
}
