package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.data.Login;
import fr.upem.net.tcp.nonblocking.server.data.MessageGlobal;
import fr.upem.net.tcp.nonblocking.server.data.PrivateMessage;
import fr.upem.net.tcp.nonblocking.server.data.PrivateRequest;
import fr.upem.net.tcp.nonblocking.server.reader.InstructionReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;

public class ClientChatos {

    static private class Context {

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
        private InstructionReader reader = new InstructionReader();
        private boolean closed = false;

        private Context(SelectionKey key){
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bbin
         *
         * The convention is that bbin is in write-mode before the call
         * to process and after the call
         *
         */
        private void processIn(ClientChatos client) {
        	for(;;){
        	    Reader.ProcessStatus status = reader.process(bbin);
         	    switch (status){
         	      case DONE:
         	          Data value = reader.get();
         	          value.decode(client);
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

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param bb
         */
        private void queueMessage(ByteBuffer bb) {
            queue.add(bb);
            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bbout from the message queue
         *
         */
        private void processOut() {
            while (!queue.isEmpty()){
                var bb = queue.peek();
                if (bb.remaining()<=bbout.remaining()){
                    queue.remove();
                    bbout.put(bb);
                }
            }
        }

        /**
         * Update the interestOps of the key looking
         * only at values of the boolean closed and
         * of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call
         * to updateInterestOps and after the call.
         * Also it is assumed that process has been be called just
         * before updateInterestOps.
         */

        private void updateInterestOps() {
            var interesOps=0;
            if (!closed && bbin.hasRemaining()){
                interesOps=interesOps|SelectionKey.OP_READ;
            }
            if (bbout.position()!=0){
                interesOps|=SelectionKey.OP_WRITE;
            }
            if (interesOps==0){
                silentlyClose();
                return;
            }
            key.interestOps(interesOps);
        }

        private void silentlyClose() {
            try {
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doRead and after the call
         *
         * @throws IOException
         */
        private void doRead(ClientChatos client) throws IOException {
        	if (sc.read(bbin)==-1) {
                System.out.println("read raté");
                closed=true;
            }
            processIn(client);
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
            bbout.flip();
            sc.write(bbout);
            bbout.compact();
            processOut();
            updateInterestOps();
        }

        public void doConnect() throws IOException {
        	if (!sc.finishConnect())
        	    return; // the selector gave a bad hint
        	key.interestOps(SelectionKey.OP_READ);
        }
    }

    static private int BUFFER_SIZE = 1024;
    static private Logger logger = Logger.getLogger(ClientChatos.class.getName());


    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private Login login;
    private String loginDemandé;
    private MessageGlobal messageGlobal;
    private final Thread console;
    private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private final HashMap<Login, PrivateRequest> hashPrivateRequest = new HashMap<>();
    private Context uniqueContext;
    private final Object lock = new Object();

    public ClientChatos(InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
        this.login = new Login();
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }

    private void consoleRun() {
        try {
            var scan = new Scanner(System.in);
            while (scan.hasNextLine()) {
                var line = scan.nextLine();
                sendCommand(line);
            }
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        } finally {
            logger.info("Console thread stopping");
        }
    }

    /**
     * Send a command to the selector via commandQueue and wake it up
     *
     * @param msg
     * @throws InterruptedException
     */
    private void sendCommand(String msg) throws InterruptedException {
    	synchronized (msg) {
            commandQueue.add(msg);
            selector.wakeup();
        }
    }
		
    /**
     * Processes the command from commandQueue
     * @throws IOException 
     */
    private void processCommands() throws IOException{
        synchronized (commandQueue) {
            while (!commandQueue.isEmpty()) {
                var command = commandQueue.remove();
                Optional<ByteBuffer> bb = parseInput(command);
                if (!bb.isPresent()) {
                    System.out.println("Something went wrong.");
                    return;
                }
                uniqueContext.queueMessage(bb.get().flip());
            }
        }
    }

    public Optional<ByteBuffer> parseInput(String input) throws IOException {
        Optional<ByteBuffer> bb;
        
        synchronized (lock) {
        	var req = ByteBuffer.allocate(BUFFER_SIZE);
	        if (login.isNotConnected()) {
	        	loginDemandé = input;
				bb = Optional.of(login.encodeLogin(input));
	        } else {
	        	if (input.startsWith("@")){ // message privé
		            var keyword = input.replaceFirst("@", "");
		            var elements = keyword.split(" ",2);
		            var msg = elements[1];
		            var msgprive = new PrivateMessage(login, new Login(elements[0]), msg);
		            bb = Optional.of(msgprive.encode(req));
		        }else if(input.startsWith("/y ") && !hashPrivateRequest.isEmpty()){
		        	var elements = input.split(" ",2);
		        	var privateRequest = hashPrivateRequest.get(new Login(elements[1]));
		        	System.out.println("Private connection with " + elements[1] + " accepted");
		        	bb = Optional.of(privateRequest.encodeAcceptPrivateRequest(req));
		        }else if(input.startsWith("/n ") && !hashPrivateRequest.isEmpty()){
		        	var elements = input.split(" ",2);
		        	var privateRequest = hashPrivateRequest.remove(new Login(elements[1]));
		        	System.out.println("Private connection refused");
		        	bb = Optional.of(privateRequest.encodeRefusePrivateRequest(req));
		        }else if (input.startsWith("/")) { // connexion privée
		        	var keyword = input.replaceFirst("/", "");
		            var elements = keyword.split(" ",2);
		            //var file = elements[1];
		            var privateRequest = new PrivateRequest(login, new Login(elements[0]));
		            bb = Optional.of(privateRequest.encodeAskPrivateRequest(req));
		        } else { // message global
		            //if (isConnected) {
		                var messageGlobal = new MessageGlobal(login, input);
		                bb = Optional.of(messageGlobal.encode(req));
		            //} else {
	//	                bb = Optional.empty();
	//	                System.out.println("Tu n'es pas connecté");
	//	            }
		        }
	        }
        }
        return bb;
    }
    
    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new Context(key);
        key.attach(uniqueContext);
        sc.connect(serverAddress);
        console.start();
        while(!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead(this);
            }
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }


    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }


    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=2){
            usage();
            return;
        }
        new ClientChatos(new InetSocketAddress(args[0],Integer.parseInt(args[1]))).launch();
    }

    private static void usage(){
        System.out.println("Usage : ClientChat hostname port");
    }
    
    public void updateLogin() {
    	login = new Login(loginDemandé);
    }
    
    public boolean isConnected(){ 
    	return !login.isNotConnected();
    }

	public void addSetPrivateRequest(PrivateRequest privateRequest) {
		synchronized (lock) {
			hashPrivateRequest.put(privateRequest.getLoginRequester(), privateRequest);
		}
		
	}
}

