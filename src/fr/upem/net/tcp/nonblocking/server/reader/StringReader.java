package fr.upem.net.tcp.nonblocking.server.reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private enum State {
        DONE, WAITING_INT, WAITING_STR, ERROR
    };

    private State state = State.WAITING_INT;
    private final ByteBuffer internalbb = ByteBuffer.allocate(1024); // write-mode
    private final IntReader intReader = new IntReader();
    private String msg;
    private int size;
    private final Charset UTF8 = StandardCharsets.UTF_8;

    public ProcessStatus process(ByteBuffer bb) {
        switch (state) {
            case WAITING_INT:
                var status = intReader.process(bb);
                switch (status) {
                    case DONE:
                        size = intReader.get();
                        if(size<0 || size>1024) {
                            return ProcessStatus.ERROR;
                        }
                        intReader.reset();
                        state = State.WAITING_STR;
                        internalbb.limit(size);
                        break;
                    case REFILL:
                        return ProcessStatus.REFILL;
                    case ERROR:
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                }
            case WAITING_STR:
                try {
                    bb.flip();
                    if (bb.remaining() <= internalbb.remaining()) {
                        internalbb.put(bb);
                    } else {
                        var oldLimit = bb.limit();
                        bb.limit(bb.position() + internalbb.remaining());
                        internalbb.put(bb);
                        bb.limit(oldLimit);
                    }
                    if (internalbb.hasRemaining()) {
                        return ProcessStatus.REFILL;
                    }
                    state = State.DONE;
                    internalbb.flip();
                    msg = UTF8.decode(internalbb).toString();

                    return ProcessStatus.DONE;
                } finally {
                    bb.compact();
                }
            default:
                throw new IllegalStateException();

        }

    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return msg;
    }

    public void reset() {
        state = State.WAITING_INT;
        internalbb.clear();
        intReader.reset();
        msg=null;
    }

}
//
//package fr.upem.net.tcp.nonblocking.server.reader;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.Charset;
//
//
//public class StringReader implements Reader<String> {
//
//	private enum State {DONE, WAITING_FOR_SIZE, WAITING_FOR_CONTENT, ERROR};
//    private final int MAX_SIZE = 1_024;
//    
//    private State state = State.WAITING_FOR_SIZE;
//    private final ByteBuffer internalbb = ByteBuffer.allocate(MAX_SIZE); // write-mode
//    private String value;
//    private int size = 0;
//    private static final Charset UTF8 = Charset.forName("UTF8");
//    private final IntReader intReader = new IntReader();
//
//    @Override
//    public ProcessStatus process(ByteBuffer bb) {
//                
//        switch(state) {
//        	case WAITING_FOR_SIZE:
//		        if(intReader.process(bb) == ProcessStatus.REFILL) {
//		        	return ProcessStatus.REFILL;
//		        }
//		        size = intReader.get();
//		        if(size < 0 || size > 1024) {
//		        	return ProcessStatus.ERROR;
//		        }
//		        state = state.WAITING_FOR_CONTENT;
//		        internalbb.limit(size);
//        	case WAITING_FOR_CONTENT:
//	            bb.flip();
//		        try {
//		            if (bb.remaining()<=internalbb.remaining()){
//		            	internalbb.put(bb);
//		            } else {
//		                var oldLimit = bb.limit();
//		                bb.limit(internalbb.remaining());
//		                internalbb.put(bb);
//		                bb.limit(oldLimit);
//		            }
//		        } finally {
//		            bb.compact();
//		        }
//		
//		        
//		    	if(!internalbb.hasRemaining()) {
//		    		internalbb.flip();
//		        	value = UTF8.decode(internalbb).toString();
//		        	state = State.DONE;
//		        	return ProcessStatus.DONE;
//		        }
//		    	return ProcessStatus.REFILL;
//		    default:
//		    	throw new IllegalStateException();
//        }
//    }
//
//    @Override
//    public String get() {
//        if (state!= State.DONE) {
//            throw new IllegalStateException();
//        }
//        return value;
//    }
//
//    @Override
//    public void reset() {
//    	intReader.reset();
//        state= State.WAITING_FOR_SIZE;
//        size = 0;
//        value = null;
//        internalbb.clear();
//    }
//}
//
