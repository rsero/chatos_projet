package fr.upem.net.tcp.nonblocking.client;

import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.Login;
import fr.upem.net.tcp.nonblocking.reader.HTTPReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

public class ContextPrivateClient implements Context {
    private final SelectionKey key;
    private final SocketChannel sc;
    private static int BUFFER_SIZE = 1024;
    private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
    private HTTPReader httpReader = new HTTPReader();
    private boolean closed = false;
    private Object lock = new Object();
    private static final Logger logger = Logger.getLogger(ContextPrivateClient.class.getName());
    private long connect_id;
    private final String directory;
    private final Charset charsetASCII = Charset.forName("ASCII");
    private final HashMap<Login, List<String>> mapFiles = new HashMap<>();

    public ContextPrivateClient(Selector selector, String directory, InetSocketAddress serverAddress, long connect_id) throws IOException {
        sc = SocketChannel.open();
        sc.configureBlocking(false);
        key = sc.register(selector, SelectionKey.OP_CONNECT);
        key.attach(this);
        sc.connect(serverAddress);
        this.connect_id=connect_id;
        this.directory=directory;
    }

    @Override
    public void processIn(ClientChatos client, SelectionKey key) throws IOException {
        for(;;) {
            ProcessStatus status = httpReader.process(bbin, key);
            switch (status) {
                case DONE:
                    Data value = httpReader.get();
                    value.decode(client, key);
                    httpReader.reset();
                    break;
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
        queue.add(bb);
        processOut();
        updateInterestOps();
    }

    @Override
    public void processOut() {
        synchronized (lock) {
            while (!queue.isEmpty()) {
                var bb = queue.peek();
                if (bb.remaining() <= bbout.remaining()) {
                    queue.remove();
                    bbout.put(bb);
                }
            }
        }
    }

    @Override
    public void updateInterestOps() {
        var interestOps = 0;
        if (!closed && bbin.hasRemaining()) {
            interestOps = interestOps | SelectionKey.OP_READ;
        }
        if (!closed && bbout.position() != 0) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interestOps == 0) {
            silentlyClose();
            return;
        }
        key.interestOps(interestOps);
    }

    @Override
    public void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    @Override
    public void doRead(ClientChatos client, SelectionKey key) throws IOException {
        if (sc.read(bbin) == -1) {
            logger.info("read raté");
            closed = true;
        }
        processIn(client, key);
        updateInterestOps();
    }

    @Override
    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        processOut();
        updateInterestOps();
    }

    @Override
    public void doConnect() throws IOException {
        if (!sc.finishConnect())
            return; // the selector gave a bad hint
        key.interestOps(SelectionKey.OP_READ);
    }

    @Override
    public void closeConnection() {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    //@Override
    public void sendCommand(Login login) {
        var files = mapFiles.get(login);
        while(!files.isEmpty()) {
            System.out.println("Un fichier est envoyé");
            var file = files.get(0);
            String request;
            try {
                request = "GET /" + file + " HTTP/1.1\r\n"
                        + "Host: " + getURL(directory) + "\r\n"
                        + "\r\n";
                var bb = charsetASCII.encode(request);
                queueMessage(bb);
            } catch (MalformedURLException e) {
                logger.warning(file + "doesn't exist");
            }
            removeFileToSend(file,files);
        }
    }

    public void addFileToMap(Login login, String file){
        if(mapFiles.putIfAbsent(login, new ArrayList<String>(Collections.singleton(file))) !=null) {
            System.out.println("la map avait déja le login vide");
            //mapFiles.get(login).add(file);
        }
    }

    public List<String> getFiles(Login login){
        return mapFiles.get(login);
    }

    public boolean correctConnectId(Long id) {
        return id != null && id.equals(connect_id);
    }

    public long getConnectId(){
        return connect_id;
    }

    public void setConnect_id(long id){
        this.connect_id=id;
    }

    private String getURL(String path) throws MalformedURLException {
        return new File(path).toURI().getPath();
    }

    public void removeFileToSend(String lastFile,List<String> files) {
        files.remove(lastFile);
    }

}
