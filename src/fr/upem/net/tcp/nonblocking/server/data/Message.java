package fr.upem.net.tcp.nonblocking.server.data;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

import java.nio.ByteBuffer;

public class Message implements Data{
    @Override
    public void processIn(ByteBuffer bbin, ServerChatos serverChatos, Context context) {

    }

    @Override
    public void processOut(ByteBuffer bbout) {

    }

    @Override
    public void broadcast(Context server) {

    }
}
