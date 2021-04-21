package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.Login;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateConnectionTransmissionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

public class ContextServer implements Context {
    private final static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<ByteBuffer> queue = new LinkedList<>();
    private final InstructionReader reader = new InstructionReader();
    private final PrivateConnectionTransmissionReader privateReader;
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Object lock = new Object();
    private final ServerDataTreatmentVisitor visitor;

    public ContextServer(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
        this.sc = (SocketChannel) key.channel();
        visitor = new ServerDataTreatmentVisitor(server, this);
        privateReader = new PrivateConnectionTransmissionReader(key);
    }

    public void processIn() throws IOException {
        boolean connectionPrivate = server.isConnectionPrivate(key);
        for (;;) {
            ProcessStatus status;
            if (connectionPrivate) {
                status = privateReader.process(bbin, key);
            } else {
                status = reader.process(bbin, key);
            }
            switch (status) {
                case DONE:
                    Data data;
                    if (connectionPrivate) {
                        data = privateReader.get();
                        privateReader.reset();
                    } else {
                        data = reader.get();
                        reader.reset();
                    }
                    data.accept(visitor);
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }
        }
    }

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

    public void processOut() {
        synchronized (lock){
            while (!queue.isEmpty()) {
               var data = queue.peek();
               if(data.remaining() <= bbout.remaining()){
                   bbout.put(data);
                   queue.remove();
               }
               else {
                   break;
               }
            }
        }
    }

    public void queueMessage(ByteBuffer data) {
        synchronized (lock){
            queue.add(data);
            processOut();
            updateInterestOps();
        }
    }

    public Context findContextClient(Login login) {
        return server.findContext(login);
    }

    public void disconnectClient(Long connect_id){
        server.removePrivateConnection(connect_id);
    }

    public SelectionKey getKey(){
        return key;
    }

    public boolean connectionReady(Long connectId) {
        return server.connectionReady(connectId);
    }
}