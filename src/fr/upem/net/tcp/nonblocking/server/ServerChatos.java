package fr.upem.net.tcp.nonblocking.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.Login;

public class ServerChatos {
    private final HashMap<String, Context> clients = new HashMap<>();//Connexion public des clients
    private final HashMap<Long, AcceptRequest> privateConnexion = new HashMap<>();//Connexion privée des clients
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final static Logger logger = Logger.getLogger(ServerChatos.class.getName());

    public ServerChatos(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
    }

    public boolean addClient(String login, Context context){
        return clients.putIfAbsent(login, context) == null;
    }
    
    public void updatePrivateConnexion(Long connectId, SelectionKey keyClient){
        privateConnexion.get(connectId).updatePrivateConnexion(keyClient);
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            printKeys();
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
        printSelectedKey(key);
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
        var context = new ContextServer(this, clientKey);
        clientKey.attach(context);
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public Context findContext(Login login) {
        return clients.get(login.getLogin());
    }
    
    public List<SelectionKey> findContext(Long connectId) {
    	var privateClient = privateConnexion.get(connectId);
        return List.of(privateClient.getKeyTarget(), privateClient.getKeyRequester());
    }

//    public boolean isConnectionPrivate(SelectionKey key) {
//    	for(var privateConnection : privateConnexion.values()) {
//    		if(privateConnection.containsKey(key)) {
//    			if(privateConnection.connexionReady())
//					return true;
//
//    		}
//    	}
//    	return false;
//    }
    
    public long definedConnectId(AcceptRequest acceptRequest) {
        Random rand = new Random();
        while(true) {
            var value = rand.nextLong();
            if(privateConnexion.putIfAbsent(value, acceptRequest) == null) {
                return value;
            }
        }
    }
    
    public boolean connectionReady(Long connectId) {
		return privateConnexion.get(connectId).connexionReady();
	}

    public static void main(String[] args) throws NumberFormatException, IOException {
        new ServerChatos(7777).launch();
    }

	private String interestOpsToString(SelectionKey key) {
	    if (!key.isValid()) {
	        return "CANCELLED";
	    }
	    int interestOps = key.interestOps();
	    ArrayList<String> list = new ArrayList<>();
	    if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
	        list.add("OP_ACCEPT");
	    if ((interestOps & SelectionKey.OP_READ) != 0)
	        list.add("OP_READ");
	    if ((interestOps & SelectionKey.OP_WRITE) != 0)
	        list.add("OP_WRITE");
	    return String.join("|", list);
	}

    public void printKeys() {
        Set<SelectionKey> selectionKeySet = selector.keys();
        if (selectionKeySet.isEmpty()) {
            System.out.println("The selector contains no key : this should not happen!");
            return;
        }
        System.out.println("The selector contains:");
        for (SelectionKey key : selectionKeySet) {
            SelectableChannel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
            } else {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key) + " "+ key.attachment().getClass());
            }
        }
    }

    private String remoteAddressToString(SocketChannel sc) {
        try {
            return sc.getRemoteAddress().toString();
        } catch (IOException e) {
            return "???";
        }
    }

    private void printSelectedKey(SelectionKey key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof ServerSocketChannel) {
            System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
        } else {
            SocketChannel sc = (SocketChannel) channel;
            System.out.println(
                    "\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
        }
    }

    private String possibleActionsToString(SelectionKey key) {
        if (!key.isValid()) {
            return "CANCELLED";
        }
        ArrayList<String> list = new ArrayList<>();
        if (key.isAcceptable())
            list.add("ACCEPT");
        if (key.isReadable())
            list.add("READ");
        if (key.isWritable())
            list.add("WRITE");
        return String.join(" and ", list);
    }


    public List<Context> contextPublic() {
		return clients.values().stream().collect(Collectors.toList());
	}
    

	public SelectionKey findKeyTarget(SelectionKey keyTarget) {
		for(var privateConnection : privateConnexion.values()) {
			SelectionKey keyFind = privateConnection.findKey(keyTarget);
    		if(keyFind != null) {
    			return keyFind;
    		}
    	}
    	return null;
	}

	public void close(Context contextServer) {
		for(var client : clients.keySet()) {
			var ctx = clients.get(client);
    		if(ctx.equals(contextServer)) {
    			clients.remove(client);
    			return;
    		}
    	}
	}

    public void removePrivateConnection(Long connectId){
        privateConnexion.get(connectId).disconnectSocket();
        privateConnexion.remove(connectId);
    }
}
