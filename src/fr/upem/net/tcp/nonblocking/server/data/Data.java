package fr.upem.net.tcp.nonblocking.server.data;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

import javax.naming.Context;
import java.nio.ByteBuffer;

public interface Data {
    //public void processIn(ByteBuffer bbin, ServerChatos serverChatos, Context context);
    public void processOut(ByteBuffer bbout);
    public void broadcast(Context server, Data data);
}
