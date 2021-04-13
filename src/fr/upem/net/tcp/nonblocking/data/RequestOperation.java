package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;

public abstract class RequestOperation implements Data{

	private final Login loginRequester;
    private final Login loginTarget;
    
	public RequestOperation(Login loginRequester, Login loginTarget) {
		this.loginRequester = loginRequester;
		this.loginTarget = loginTarget;
	}
    
    public boolean processOut(ByteBuffer bb) throws IOException {
		return bb != null;
	}
    
    ContextServer findContextRequester(ContextServer context) {
    	return context.findContextClient(loginRequester);
    }
    
    ContextServer findContextTarget(ContextServer context) {
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

	public void deleteRequestConnection(ClientChatos client) {
		client.deleteRequestConnection(loginTarget);
	}
}
