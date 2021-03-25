package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.server.reader.LoginReader;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
;

public class ServerConsole {

    private static Logger logger = Logger.getLogger(ServerConsole.class.getName());

    public static void connect(ByteBuffer bbin, ServerChatos serverChatos, Context context){
        var loginReader = new LoginReader();
        loginReader.process(bbin);
        var login = loginReader.get();

        if(serverChatos.addClient(login.getLogin(), context)){
            logger.info("Client ajouté avec succès");
        }
        else {
            logger.info("Client refusé car login déja utilisé");
        }
    }
}
