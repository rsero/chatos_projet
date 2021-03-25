package fr.upem.net.tcp.nonblocking.server.data;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public interface Data {
    //public void processIn(ByteBuffer bbin, ServerChatos serverChatos, Context context);
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server);
    //public void broadcast();
}
