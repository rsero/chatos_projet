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
    private final PrivateConnectionTransmissionReader privateConnectionTransmissionReader;
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Object lock = new Object();
    private boolean isPrivate = false;
    private final ServerDataTreatmentVisitor visitor;
    private Charset UTF8 = StandardCharsets.UTF_8;

    public ContextServer(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
        this.sc = (SocketChannel) key.channel();
        visitor = new ServerDataTreatmentVisitor(server, this);
        privateConnectionTransmissionReader = new PrivateConnectionTransmissionReader(key);
    }

    public void processIn() throws IOException {
        boolean connectionPrivate = server.isConnectionPrivate(key);
        for (;;) {
            ProcessStatus status;
            if (connectionPrivate) {
                status = privateConnectionTransmissionReader.process(bbin, key);
                System.out.println(status);
            } else {
                status = reader.process(bbin, key);
            }
            switch (status) {
                case DONE:
                    Data data;
                    if (connectionPrivate) {
                        data = privateConnectionTransmissionReader.get();
                        privateConnectionTransmissionReader.reset();
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
            System.out.println("OPREAD");
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }
        if (!closed && bbout.position() > 0) {
            System.out.println("OPWRITE");
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
        System.out.println("do read");
        if (sc.read(bbin) == -1) {
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    public void doWrite() throws IOException {
        System.out.println("do write");
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
                   System.out.println(queue.size());
               }
               else {
                   break;
               }
            }
        }
    }

    public void queueMessage(ByteBuffer data) {
        synchronized (lock){
            System.out.println("queue message");
            queue.add(data);
            System.out.println("queue message apres add");
            processOut();
            updateInterestOps();
            System.out.println("fin queue message");
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

    public ContextPrivateServer contextToPrivateContext(){
        var context = new ContextPrivateServer(server, key, sc, bbout);
        key.attach(context);
        return context;
    }

    public boolean connectionReady(Long connectId) {
        return server.connectionReady(connectId);
    }

    public void setPrivate(){
        isPrivate = true;
    }
}