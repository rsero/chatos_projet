package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Login implements Data {
    //private static final LoginReader loginReader = new LoginReader();
	private static final Charset UTF8 = Charset.forName("UTF8");
	private static int BUFFER_SIZE = 34;
    private final String name;
    private Byte byteReceive;

    public Login(String name){
        this.name= Objects.requireNonNull(name);
    }
    
    public Login(){
    	name = "";
    }

    public String getLogin(){
        return name;
    }
    
    public boolean isNotConnect() {
    	if(name.equals(""))
    		return true;
    	return false;
    }

    @Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
    	if(!bbout.hasRemaining()) {
    		return false;
    	}
    	if(server.addClient(name, context)) {
    		bbout.put((byte) 1);
    	}
    	else {
    		bbout.put((byte) 2);
    	}
    	return true;
    }

	@Override
	public String toString() {
		return name;
	}
    
	public Optional<ByteBuffer> encodeLogin(SocketChannel sc, String log) throws IOException {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
		var loginbuff = UTF8.encode(log);
		int len = loginbuff.remaining();
		if (BUFFER_SIZE < len + Integer.BYTES + 1) {
			return Optional.empty();
		}
		req.put((byte) 5);
		req.putInt(len);
		req.put(loginbuff);
		req.flip();
		return Optional.of(req);
	}

	@Override
	public void decode() {
		// TODO Auto-generated method stub
		if (byteReceive == (byte) 1) {
			System.out.println("Connexion accepté");
			//mettre à jour le login
		}
		else if(byteReceive == (byte) 2) {
			System.out.println("Login deja existant");
		}
//			login = log;
//			return;
//		}
	}
}
