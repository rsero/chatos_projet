package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import fr.upem.net.tcp.nonblocking.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.Login;
import fr.upem.net.tcp.nonblocking.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

public class ContextServer {
    private final static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<Data> queue = new LinkedList<>();
    private final InstructionReader reader = new InstructionReader();
    private final PrivateConnexionTransmissionReader privateConnexionTransmissionReader;
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Object lock = new Object();

    public ContextServer(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
        this.sc = (SocketChannel) key.channel();
        privateConnexionTransmissionReader = new PrivateConnexionTransmissionReader(key);
    }

    private void updateInterestOps() {
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

    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void doRead(SelectionKey key) throws IOException {
        if (sc.read(bbin) == -1) {
            closed = true;
        }
        processIn(key);
        updateInterestOps();
    }

    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        processOut();
        updateInterestOps();
    }

    private void processIn(SelectionKey key) throws IOException {
        boolean connectionPrivate = server.isConnectionPrivate(key);
        for (var cpt =0 ; ; cpt++) {
            ProcessStatus status;
            if (connectionPrivate && cpt == 0) {
                privateConnexionTransmissionReader.reset();
                status = privateConnexionTransmissionReader.process(bbin);
            } else {
                status = reader.process(bbin);
            }
            switch (status) {
                case DONE:
                    Data data;
                    if (connectionPrivate && cpt == 0) {
                        data = (Data) privateConnexionTransmissionReader.get();
                        privateConnexionTransmissionReader.reset();
                        server.broadcast(data);
                        return;
                    } else {
                        data = (Data) reader.get();
                        reader.reset();
                        server.broadcast(data);
                    }
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }

        }
    }

    private void processOut() throws IOException {
        synchronized (lock){
            while (!queue.isEmpty()) {
               var data = queue.peek();
               if(data.processOut(bbout, this, server)) {
                   queue.remove();
               }
            }
        }
    }

    public void queueMessage(Data data) throws IOException {
        synchronized (lock){
            queue.add(data);
            processOut();
            updateInterestOps();
        }
    }

    public ContextServer findContextClient(Login login) {
        return server.findContext(login);
    }

    public long definedConnectId(AcceptRequest acceptRequest) {
        return server.definedConnectId(acceptRequest);
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

    public SelectionKey findKeyTarget(SelectionKey keyTarget) {
        return server.findKeyTarget(keyTarget);
    }

    public void disconnectClient(Long connect_id){
        server.removePrivateConnection(connect_id);
    }

    public SelectionKey getKey(){
        return key;
    }
}