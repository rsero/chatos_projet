package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.reader.LoginReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;
import fr.upem.net.tcp.nonblocking.server.reader.StringReader;

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

        bbin.flip();
        if(bbin.hasRemaining()){

        }
        var opCode = bbin.get();
        processIn(opCode);
        updateInterestOps();
    }

    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        processOut();
        updateInterestOps();
    }

    private void processIn(Byte opCode) {
        switch(opCode) {
            case 0:
                read(new LoginReader());
                //login.processIn(bbin, server, this);
            case 1:
                read(new StringReader());
        }
    }

    private void read(Reader reader){
        for (;;) {
            Reader.ProcessStatus status = reader.process(bbin);
            switch (status) {
                case DONE:
                    var data = (Data) reader.get();
                    //data.broadcast(this);
                    reader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    //silentlyClose();
                    return;
            }
        }
    }

    private void processOut() {
        while (!queue.isEmpty()) {
            var data = queue.remove();
            data.processOut(bbout);
        }
    }

    private void queueMessage(Data data) {
        queue.add(data);
        processOut();
        updateInterestOps();
    }
}
