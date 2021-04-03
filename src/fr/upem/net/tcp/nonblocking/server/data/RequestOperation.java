package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public abstract class RequestOperation implements Data{

	private final Login loginRequester;
    private final Login loginTarget;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
	public RequestOperation(Login loginRequester, Login loginTarget) {
		this.loginRequester = loginRequester;
		this.loginTarget = loginTarget;
	}
    
    public boolean processOut(ByteBuffer bb) throws IOException {
    	if (bb==null) {
    		return false;
    	}
    	return true;
    }
    
    Context findContextRequester(Context context) {
    	return context.findContextClient(loginRequester);
    }
    
    Context findContextTarget(Context context) {
    	return context.findContextClient(loginTarget);
    }
    
    public Login getLoginRequester() {
    	return loginRequester;
    }
    
    public Login getLoginTarget() {
    	return loginTarget;
    }
	
    public String loginRequester() {
    	return loginRequester.getLogin();
    }
    
    public String loginTarget() {
    	return loginTarget.getLogin();
    }
}
