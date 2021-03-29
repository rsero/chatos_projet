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
        if (!closed && bbin.hasRemaining()) {
            newInterestOps = newInterestOps | SelectionKey.OP_READ;
        }

        if (bbout.position() > 0) {
            newInterestOps = newInterestOps | SelectionKey.OP_WRITE;
        }
        if (newInterestOps == 0) {
            silentlyClose();
//            return;
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

        //bbin.flip();
//        if(!bbin.hasRemaining()){
//            return;
//        }
          //Byte opCode = bbin.get();
//        int inttest = bbin.getInt();
//        var bb = Charset.forName("UTF-8").decode(bbin);
//        System.out.println("Byte >> " + opCode + "\nlen >> " + inttest + "\nmessage >>" + bb + "\n");
//      
//        bbin.putInt(inttest);
//        bbin.put(Charset.forName("UTF-8").encode(bb));
//        bbin.flip();
        //processIn(opCode);
        processIn((byte) 0);
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
    	System.out.println("processin");
//        switch(opCode) {
//            case 0:
            	//var len = bbin.getInt();
            	//System.out.println("in est la : " + len);
//            	bbin.compact();
            	
           //     read(new InstructionReader());
                //login.processIn(bbin, server, this);
//            case 1:
//                read(new StringReader());
//        }
    	//for (;;) {
        	System.out.println("debut read");
        	Reader.ProcessStatus status = reader.process(bbin);
        	System.out.println("fin du read");
        
            switch (status) {
                case DONE:
                	System.out.println("reader done");
                    var data = (Data) reader.get();
                    System.out.println(">>>>>" + data.toString());
                    server.broadcast(data);
                    System.out.println("Jepasse le broadcast");
                    reader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    return;
            }
        //}
    }

    private void read(Reader<?> reader){
//        for (;;) {
//        	//System.out.println("debut read");
//        	Reader.ProcessStatus status = reader.process(bbin);
//         
//        
//            switch (status) {
//                case DONE:
//                	System.out.println("reader done");
//                    var data = (Data) reader.get();
//                    System.out.println(data.toString());
//                    server.broadcast(data);
//                    reader.reset();
//                    break;
//                case REFILL:
//                    return;
//                case ERROR:
//                    silentlyClose();
//                    return;
//            }
//        }
    }

    private void processOut() {
    	//bbout.flip();
        while (!queue.isEmpty()) {
            var data = queue.peek();
            //System.out.println("debut process out 1");
            if(data.processOut(bbout, this, server)) {
            	queue.remove();
            }
            //System.out.println("fin process out 1");
        }
    }

    public void queueMessage(Data data) {
        queue.add(data);
        System.out.println("debut du processout");
        processOut();
        System.out.println("fin du processout");
        updateInterestOps();
    }
}
