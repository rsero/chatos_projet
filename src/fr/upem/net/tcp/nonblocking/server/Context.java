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
        System.out.println("etape4");

        if (!closed && bbin.hasRemaining()) {
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }
        System.out.println("etape5");

        if (bbout.position() > 0) {
            newInterestOps = newInterestOps | SelectionKey.OP_WRITE;
        }
        
        System.out.println("etape6");

        if (newInterestOps == 0) {
            silentlyClose();
            return;
        }
        
        System.out.println("etape7 : " + newInterestOps);

        key.interestOps(newInterestOps);
        
        System.out.println("etape8");

    }

    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void doRead() throws IOException {
        System.out.println("etape0");
        if (sc.read(bbin) == -1) {
            closed = true;
        }
        System.out.println("etape1");
        processIn();
        System.out.println("etape2");
        updateInterestOps();
        System.out.println("etape3");
    }

    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        processOut();
        updateInterestOps();
    }

    private void processIn() {
    	System.out.println("je suis dans ");
    	//
    	bbin.flip();
    	for(;;) {
	        Reader.ProcessStatus status = reader.process(bbin);
	        System.out.println("Je suis plus dedans");
	        switch (status) {
	            case DONE:
	                var data = (Data) reader.get();
	                System.out.println("Data >>" + data);
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
    }

    private void processOut() {
    	//bbout.flip();
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
