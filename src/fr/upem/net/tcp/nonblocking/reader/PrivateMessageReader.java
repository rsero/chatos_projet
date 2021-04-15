package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.Login;
import fr.upem.net.tcp.nonblocking.data.PrivateMessage;

public class PrivateMessageReader implements Reader<PrivateMessage> {
    private enum State {
        DONE, WAITING_SENDER_LOGIN, WAITING_TARGET_LOGIN , WAITING_MSG, ERROR
    }

    private State state = State.WAITING_SENDER_LOGIN;
    private String msg;
    private Login senderLogin;
    private Login targetLogin;
    private final LoginReader loginReader = new LoginReader();
    private final StringReader messageReader = new StringReader();

    @Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_SENDER_LOGIN) {
            var processlogin = loginReader.process(bb, key);
            switch (processlogin) {
                case DONE:
                    senderLogin = loginReader.get();
                    loginReader.reset();
                    state = State.WAITING_TARGET_LOGIN;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_TARGET_LOGIN) {
            var processlogin = loginReader.process(bb, key);
            switch (processlogin) {
                case DONE:
                    targetLogin = loginReader.get();
                    loginReader.reset();
                    state = State.WAITING_MSG;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if(state ==State.WAITING_MSG) {
            var processMsg = messageReader.process(bb, key);
            switch(processMsg) {
                case DONE:
                    msg=messageReader.get();
                    state = State.DONE;
                    break;
                case REFILL :
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        return ProcessStatus.DONE;

    }

    @Override
    public PrivateMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new PrivateMessage(senderLogin, targetLogin, msg);
    }

    @Override
    public void reset() {
        state = State.WAITING_SENDER_LOGIN;
    }

}