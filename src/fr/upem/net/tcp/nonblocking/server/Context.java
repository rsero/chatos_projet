package fr.upem.net.tcp.nonblocking.server;

import java.nio.channels.SelectionKey;

public class Context {

    private final ServerChatos server;
    private final SelectionKey key;

    public Context(ServerChatos server, SelectionKey key) {
        this.server=server;
        this.key=key;
    }
}
