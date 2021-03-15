package fr.upem.net.tcp.nonblocking.server;

import java.nio.channels.SelectionKey;
import java.util.HashMap;

public class ServerChatos {
    private final HashMap<String, SelectionKey> clients = new HashMap<>();

}
