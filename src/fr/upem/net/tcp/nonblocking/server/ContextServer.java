package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.server.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.data.Login;
import fr.upem.net.tcp.nonblocking.server.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ContextServer {
    private static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<Data> queue = new LinkedList<>();
    private InstructionReader reader = new InstructionReader();
    private boolean closed = false;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);

    public ContextServer(ServerChatos server, SelectionKey key) {
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
    	for(;;) {
	        Reader.ProcessStatus status = reader.process(bbin);
	        switch (status) {
	            case DONE:
	                var data = (Data) reader.get();
	                server.broadcast(data, this, key);
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

    private void processOut() throws IOException {
        while (!queue.isEmpty()) {
            var data = queue.peek();
            if(data.processOut(bbout, this, server)) {
            	queue.remove();
            }
        }
    }

    public void queueMessage(Data data) throws IOException {
        queue.add(data);
        processOut();
        updateInterestOps();
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

	public List<ContextServer> contextPublic() {
		return server.contextPublic();
	}
}
