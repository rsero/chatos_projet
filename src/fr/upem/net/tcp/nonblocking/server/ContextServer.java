package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.Login;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

public class ContextServer implements Context {
    private final static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<ByteBuffer> queue = new LinkedList<>();
    private final InstructionReader reader = new InstructionReader();
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Object lock = new Object();

    public ContextServer(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
        this.sc = (SocketChannel) key.channel();
    }

    public void processIn() throws IOException {
        for (;;) {
            System.out.println("processin public dans le for");
            ProcessStatus status;
            status = reader.process(bbin, key);
            switch (status) {
                case DONE:
                    Data data = (Data) reader.get();
                    System.out.println("je clear le bb");
                    reader.reset();
                    server.broadcast(data, this);
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
        System.out.println("dowrite public context server");
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

    public SelectionKey findKeyTarget(SelectionKey keyTarget) {
        return server.findKeyTarget(keyTarget);
    }

    public long definedConnectId(AcceptRequest acceptRequest) {
        return server.definedConnectId(acceptRequest);
    }

    public void disconnectClient(Long connect_id){
        server.removePrivateConnection(connect_id);
    }

    public SelectionKey getKey(){
        return key;
    }

    public ContextPrivateServer contextToPrivateContext(){
        var context = new ContextPrivateServer(server, key, sc, bbout);
        key.attach(context);
        return context;
    }
}