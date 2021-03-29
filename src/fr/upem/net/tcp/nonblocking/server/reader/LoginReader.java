package fr.upem.net.tcp.nonblocking.server.reader;
import fr.upem.net.tcp.nonblocking.server.data.Login;

import java.nio.ByteBuffer;

public class LoginReader implements Reader<Login> {

    private enum State {
        DONE, WAITING_STR, ERROR
    };
    private State state = State.WAITING_STR;
    private static final int BUFFER_SIZE = 34;
    private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private StringReader stringReader = new StringReader();
    private Login login;
    
    public Reader.ProcessStatus process(ByteBuffer bb) {
        
        var processlogin = stringReader.process(bb);
        switch (processlogin) {
            case DONE:
                var log = stringReader.get();
                System.out.println("log >>><<< " + log + "\n");
                login = new Login(log);
                stringReader.reset();
                state = State.DONE;
                break;
            case REFILL:
                System.out.println("REFILL du loginreader");
                return ProcessStatus.REFILL;
            case ERROR:
                state= State.ERROR;
                return ProcessStatus.ERROR;
        }
        if(state == State.WAITING_STR) {
        	System.out.println("waiting_str");
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Login get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return login;
    }

    @Override
    public void reset() {
        state = LoginReader.State.WAITING_STR;
        internalbb.clear();
    }
}
//
//import fr.upem.net.tcp.nonblocking.server.data.Login;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//
//public class LoginReader implements Reader<Login> {
//
//    private enum State {
//        DONE, WAITING_STR, ERROR
//    };
//    private State state = State.WAITING_STR;
//    private static final int BUFFER_SIZE = 34;
//    private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
//    private StringReader stringReader = new StringReader();
//    private String loginName;
//    private int loginSize;
//    private Login login;
//    private final Charset UTF8 = StandardCharsets.UTF_8;
//
//    public Reader.ProcessStatus process(ByteBuffer bb) {
//    	
//    	var processlogin = stringReader.process(bb);
////    	 int inttest = bb.getInt();
////         var bbtest = Charset.forName("UTF-8").decode(bb);
////         System.out.println("len >> " + inttest + "\nmessage >>" + bbtest + "\n");
////        
//    	switch (processlogin) {
//            case DONE:
//                var log = stringReader.get();
//                login = new Login(log);
//                //stringReader.reset();
//                break;
//            case REFILL:
//                System.out.println("REFILL du loginreader");
//                return ProcessStatus.REFILL;
//
//            case ERROR:
//                return ProcessStatus.ERROR;
//        }return ProcessStatus.DONE;
//    }
//
//    @Override
//    public Login get() {
//        if (state != LoginReader.State.DONE) {
//            throw new IllegalStateException();
//        }
//        return login;
//    }
//
//    @Override
//    public void reset() {
//        state = LoginReader.State.WAITING_STR;
//        internalbb.clear();
//        stringReader.reset();
//    }
//
//}
//
