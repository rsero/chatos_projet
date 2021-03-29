package fr.upem.net.tcp.nonblocking.server.data;


import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Message implements Data{

    private final String login;
    private final String msg;

    public Message(String login, String msg) {
        this.login=login;
        this.msg=msg;
    }

    public String getLogin() {
        return login;
    }

    public String getMsg() {
        return msg;
    }

    public String toString() {
        return login + " : " + msg;
    }

	@Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
		return false;
    }


}
