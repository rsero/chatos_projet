package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.MessageGlobal;

import java.nio.ByteBuffer;

public class MessageReader implements Reader<MessageGlobal> {
    private enum State {
        DONE, WAITING_LOGIN, WAITING_MSG, ERROR
    };

    private State state = State.WAITING_LOGIN;
    private String msg;
    private String login;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        StringReader sr = new StringReader();
        if (state == State.WAITING_LOGIN) {
            var processlogin = sr.process(bb);
            switch (processlogin) {
                case DONE:
                    login = sr.get();
                    sr.reset();
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
            var processmsg = sr.process(bb);
            switch(processmsg) {
                case DONE:
                    msg=sr.get();
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
    public MessageGlobal get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return null;
        //return new MessageGlobal(login, msg);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
    }

}