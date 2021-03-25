package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.server.data.Data;

public class ServerChatos {
    private final HashMap<String, Context> clients = new HashMap<>();
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private static Logger logger = Logger.getLogger(ServerChatos.class.getName());

    public ServerChatos() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(7777));
        selector = Selector.open();
    }

    public boolean addClient(String login, Context context){
        if(clients.putIfAbsent(login, context) == null){
            return false;
        }
        return true;
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
            System.out.println("Select finished");
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
        try {
            if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", e);
            silentlyClose(key);
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var client = serverSocketChannel.accept();
        if (client == null) {
            logger.warning("The selector was wrong");
            return;
        }
        client.configureBlocking(false);
        var clientKey = client.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new Context(this, clientKey));
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }
    
    public void broadcast(Data data) {
    	for (SelectionKey key : selector.keys()){
            var ctx = (Context) key.attachment();
            if (ctx==null)
                continue;
            ctx.queueMessage(data);
        }
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
//        if (args.length!=1){
//            usage();
//            return;
//        }
        new ServerChatos().launch();
    }

//    private static void usage(){
//        System.out.println("Usage : ServerChatos port");
//    }

}
