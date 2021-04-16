package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
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
    private final ByteBuffer bbout;
    private final Object lock = new Object();

    public ContextPrivateServer(ServerChatos server, SelectionKey key, SocketChannel sc, ByteBuffer bbout){
        this.server=server;
        this.key = key;
        this.sc = sc;
        this.bbout = bbout;
        privateConnexionTransmissionReader = new PrivateConnexionTransmissionReader(key);
    }

    @Override
    public void processIn() throws IOException {
        System.out.println("processin private hors du for");
        for (;;) {
            System.out.println("processin private  dans le for");
            ProcessStatus status;
            privateConnexionTransmissionReader.reset();
            status = privateConnexionTransmissionReader.process(bbin, key);
            switch (status) {
                case DONE:
                    Data data = (Data) privateConnexionTransmissionReader.get();
                    privateConnexionTransmissionReader.reset();
                    //data.accept(visitor);
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
        System.out.println("queuemessage context prive server");
        synchronized (lock){
            System.out.println("queuemessage context prive server avant add : "+ queue.size());
            queue.add(bb);
            System.out.println("queuemessage context prive server apres add : "+ queue.size());
            processOut();
            updateInterestOps();
        }
    }

    @Override
    public void processOut() {
        System.out.println("processout context prive server avant : "+queue.size());
        synchronized (lock){
            while (!queue.isEmpty()) {
                var data = queue.peek();
                if(data.remaining() <= bbout.remaining()){
                    bbout.put(data);
                    queue.remove();
                }
            }
        }System.out.println("processout context prive server apres : "+queue.size());
    }

    @Override
    public void updateInterestOps() {
        int newInterestOps = 0;
        if (!closed && bbin.hasRemaining()) {
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }
        if (!closed && bbout.position() > 0) {
            System.out.println("updateinterest OP_WRITE");
            newInterestOps = newInterestOps | SelectionKey.OP_WRITE;
        }
        if (newInterestOps == 0) {
            server.close(this);
            silentlyClose();
            return;
        }
        System.out.println("interestop context prive serveur "+ newInterestOps);
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
        System.out.println("dowrite private context server");
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

    public SelectionKey getKey(){
        return key;
    }

    public boolean connectionReady(Long connectId) {
        return server.connectionReady(connectId);
    }

    public List<SelectionKey> findContext(Long connectId) {
        return server.findContext(connectId);
    }

    public void updatePrivateConnexion(Long connectId, SelectionKey keyClient) {
        server.updatePrivateConnexion(connectId, keyClient);

    }
}
