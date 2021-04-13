package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import fr.upem.net.tcp.nonblocking.server.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.data.Login;
import fr.upem.net.tcp.nonblocking.server.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.server.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.server.reader.ProcessStatus;

public class ContextServer {
    private static int BUFFER_SIZE = 1024;
    private final ServerChatos server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final Queue<Data> queue = new LinkedList<>();
    private InstructionReader reader = new InstructionReader();
    private PrivateConnexionTransmissionReader privateConnexionTransmissionReader = new PrivateConnexionTransmissionReader();
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
                System.out.println("privéee1");
                privateConnexionTransmissionReader.reset();
                status = privateConnexionTransmissionReader.process(bbin);
            } else {
                System.out.println("pasprivéee1");
                status = reader.process(bbin);
            }
                switch (status) {
                    case DONE:
                        Data data;
                        if (connectionPrivate && cpt == 0) {
                            System.out.println("privéee2");
                            data = (Data) privateConnexionTransmissionReader.get();
                            System.out.println(data);
                            privateConnexionTransmissionReader.reset();
                            System.out.println("commence broad");
                            server.printKeys();
                            server.broadcast(data, this, key);
                            server.printKeys();
                            System.out.println("fin broad");
                            return;
                        } else {
                            System.out.println("pasprivéee2");
                            data = (Data) reader.get();
                            reader.reset();
                            System.out.println("commence broad");
                            server.broadcast(data, this, key);
                            System.out.println("fin broad");
                        }
                        break;
                    case REFILL:
                        System.out.println("je refill");
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

	public SelectionKey findKeyTarget(SelectionKey keyTarget) {
		return server.findKeyTarget(keyTarget);
	}

	public void disconnectClient(Long connectId){
        server.removePrivateConnection(connectId);
    }
}
