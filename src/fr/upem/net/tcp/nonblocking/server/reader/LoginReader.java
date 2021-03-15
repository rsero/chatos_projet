package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.Login;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LoginReader implements Reader<Login> {

    private enum State {
        DONE, WAITING_INT, WAITING_STR, ERROR
    };

    private static final int BUFFER_SIZE = 34;
    private LoginReader.State state = LoginReader.State.WAITING_INT;
    private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private final IntReader intReader = new IntReader();
    private String loginName;
    private int loginSize;
    private Login login;
    private final Charset UTF8 = StandardCharsets.UTF_8;

    public Reader.ProcessStatus process(ByteBuffer bb) {
        switch (state) {
            case WAITING_INT:
                var status = intReader.process(bb);
                switch (status) {
                    case DONE:
                        loginSize = intReader.get();
                        if(loginSize <0 || loginSize > BUFFER_SIZE - Integer.BYTES) {
                            return Reader.ProcessStatus.ERROR;
                        }
                        intReader.reset();
                        state = LoginReader.State.WAITING_STR;
                        internalbb.limit(loginSize);
                        break;
                    case REFILL:
                        return Reader.ProcessStatus.REFILL;
                    case ERROR:
                        state = LoginReader.State.ERROR;
                        return Reader.ProcessStatus.ERROR;
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
                        return Reader.ProcessStatus.REFILL;
                    }
                    state = LoginReader.State.DONE;
                    internalbb.flip();
                    loginName = UTF8.decode(internalbb).toString();
                    login = new Login(loginName);
                    return Reader.ProcessStatus.DONE;
                } finally {
                    bb.compact();
                }
            default:
                throw new IllegalStateException();

        }

    }

    @Override
    public Login get() {
        if (state != LoginReader.State.DONE) {
            throw new IllegalStateException();
        }
        return login;
    }

    @Override
    public void reset() {
        state = LoginReader.State.WAITING_INT;
        internalbb.clear();
    }

}
