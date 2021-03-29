package fr.upem.net.blocking.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientChatos {

    private static final int BUFFER_SIZE = 1024;
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static String login = "";
    static private Logger logger = Logger.getLogger(ClientChatos.class.getName());

    public static String input(Scanner scan) {
        String str = "";
        while (scan.hasNextLine()) {
//            if(str.equals(""))
//                break;
            str = scan.nextLine();
            return str;
        }
        return str;
    }
    /*
    private static boolean checkLogin(List<String> list, String response) {
        var sj = list.stream().collect(Collectors.joining(","));
        return sj.equals(response);
    }
    */
    /**
     * Write all the strings in list on the server and read the long sent
     * by the server and returns it
     *
     * returns Optional.empty if the protocol is not followed by the server but no
     * IOException is thrown
     *
     * @param sc
     * @return
     * @throws IOException
     */
    private static Optional<String> requestLogin(SocketChannel sc, String log) throws IOException {
        var req = ByteBuffer.allocate(BUFFER_SIZE);
        var loginbuff = UTF8.encode(log);
        int len = loginbuff.remaining();
        if(BUFFER_SIZE < len + Integer.BYTES + 1) {
            return Optional.empty();
        }
        req.put((byte) 0);
        req.putInt(len);
        req.put(loginbuff);
        req.flip();
        sc.write(req);
        var rep = ByteBuffer.allocate(Byte.BYTES);
        if(!readFully(sc,rep)) {
            return Optional.empty();
        }
        rep.flip();
        var answer = rep.get();
        if(answer==(byte) 1){
            login = log;
            return Optional.of(log);
        }
        return Optional.empty();
    }


    static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
        while(bb.hasRemaining()) {
            if (sc.read(bb)==-1){
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        InetSocketAddress server = new InetSocketAddress(args[0], Integer.valueOf(args[1]));
        try (SocketChannel sc = SocketChannel.open(server);Scanner scan = new Scanner(System.in)) {
            Optional<String> l = Optional.empty();
            if(login.equals("")){
                var log = input(scan);
                l = requestLogin(sc,log);
            } else {
                // autres requêtes à faire
            }
            if (!l.isPresent()) {
                System.err.println("Connection with server lost.");
                return;
            }
            /*
            if (!checkString(liste, l.get())) {
                System.err.println("Oups! Something wrong happens!");
            }

             */
        }
        System.err.println("Everything seems ok");
    }

}
