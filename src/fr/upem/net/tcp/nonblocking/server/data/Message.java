package fr.upem.net.tcp.nonblocking.server.data;


import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Message implements Data{

	@Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
		return false;
    }


}
