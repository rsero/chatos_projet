package fr.upem.net.tcp.nonblocking.server.data;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Login implements Data {
    //private static final LoginReader loginReader = new LoginReader();
    private final String name;

    public Login(String name){
        this.name= Objects.requireNonNull(name);
    }

    public String getLogin(){
        return name;
    }

    @Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
    	
    	System.out.println("bbout size " + bbout.remaining() + "\n");
    	if(!bbout.hasRemaining()) {
    		System.out.println("C'est sur");
    		return false;
    	}
    	System.out.println("on passe quand meme la de temps en temps");
    	if(server.addClient(name, context)) {
    		System.out.println("On accepte le nom");
    		bbout.put((byte) 1);
    		System.out.println("c'est cool");
    	}
    	else {
    		System.out.println("On refuse le nom");
    		bbout.put((byte) 2);
    	}
    	return true;
    }

	@Override
	public String toString() {
		return "Login [name=" + name + "]";
	}
    
    
}
