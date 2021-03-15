package fr.upem.net.tcp.nonblocking.server.data;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;
import fr.upem.net.tcp.nonblocking.server.reader.LoginReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Login implements Data {
    private final LoginReader loginReader = new LoginReader();
    private final String name;
    public Login(String name){
        this.name= Objects.requireNonNull(name);
    }
    @Override
    public static void processIn(ByteBuffer bbin, ServerChatos serverChatos, Context context) {
        for (;;) {
            Reader.ProcessStatus status = loginReader.process(bbin);
            switch (status) {
                case DONE:
                    Login data = loginReader.get();
                    //server.broadcast(data);
                    //mr.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    //silentlyClose();
                    return;
            }
        }
    }

    @Override
    public void processOut(ByteBuffer bbout) {

        //bbout.putInt(UTF8.encode(msg.getLogin()).remaining());
        //bbout.put(UTF8.encode(msg.getLogin()));
        //bbout.putInt(UTF8.encode(msg.getMsg()).remaining());
        //bbout.put(UTF8.encode(msg.getMsg()));
    }
}
