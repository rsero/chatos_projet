package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.*;

import java.io.IOException;

public class ServerDataTreatmentVisitor implements DataVisitor {

    private ServerChatos server;
    private Context context;

    public ServerDataTreatmentVisitor(ServerChatos server,Context context){
        this.server = server;
        this.context = context;
    }

    @Override
    public void visit(Login login) throws IOException {
        if(login.processOut(context, server)){
            context.queueMessage(login.encode((byte)1).flip());
        } else {
            context.queueMessage(login.encode((byte)2).flip());
        }
    }

    @Override
    public void visit(OpCode opCode) throws IOException {
        //do nothing
    }

    @Override
    public void visit(AcceptRequest acceptRequest) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        acceptRequest.setConnect_id(server.definedConnectId(acceptRequest));
        var ctx = acceptRequest.findContextRequester(contextServer);
        ctx.queueMessage(acceptRequest.encode().flip());
        ctx = acceptRequest.findContextTarget(contextServer);
        ctx.queueMessage(acceptRequest.encode().flip());
    }

    @Override
    public void visit(DisconnectRequest disconnectRequest) {
        ContextServer contextServer = (ContextServer) context;
        contextServer.disconnectClient(disconnectRequest.getConnectId());
        var ctx = server.findContext(disconnectRequest.getLoginTarget());
        //ctx.queueMessage(disconnectRequest.encode().flip());
    }

    @Override
    public void visit(DisconnectRequestConnection disconnectRequestConnection) {
        ContextServer contextServer = (ContextServer) context;
        //var ctx = disconnectRequestConnection.findContextRequester(contextServer);
        //ctx.queueMessage(disconnectRequestConnection.encode().flip());
    }

    @Override
    public void visit(HTTPError httpError) {
        //pas de broadcast
    }

    @Override
    public void visit(HTTPFile httpFile) {
        //pas de broadcast
    }

    @Override
    public void visit(HTTPRequest httpRequest) {
        //pas de broadcast
    }

    @Override
    public void visit(MessageGlobal messageGlobal) throws IOException {
        var bb = messageGlobal.encode();
        if (bb==null) {
            return;
        }
        for (Context ctx : server.contextPublic()){
            ctx.queueMessage(bb.flip());
        }
    }

    @Override
    public void visit(PrivateConnectionTransmission privateConnectionTransmission) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var keyTarget = server.findKeyTarget(contextServer.getKey());
        ((ContextServer) keyTarget.attachment()).queueMessage(privateConnectionTransmission.encode());
    }

    @Override
    public void visit(PrivateLogin privateLogin) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        server.updatePrivateConnexion(privateLogin.getConnectId(), contextServer.getKey());
        if(!contextServer.connectionReady(privateLogin.getConnectId())) {
            return;
        }
        var contexts = server.findContext(privateLogin.getConnectId());
        var response = privateLogin.encodeResponse();
        ((Context) contexts.get(0).attachment()).queueMessage(response.flip());
        ((Context) contexts.get(1).attachment()).queueMessage(response.flip());
    }

    @Override
    public void visit(PrivateMessage privateMessage) throws IOException {
        var ctx = server.findContext(privateMessage.getLoginTarget());
        ctx.queueMessage(privateMessage.encode().flip());
    }

    @Override
    public void visit(PrivateRequest privateRequest) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var ctx = privateRequest.findContextTarget(contextServer);
        if(ctx != null)
            ctx.queueMessage(privateRequest.encode().flip());
        else
            System.out.println("This client is not connected");
    }

    @Override
    public void visit(RefuseRequest refuseRequest) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var ctx = refuseRequest.findContextRequester(contextServer);
        ctx.queueMessage(refuseRequest.encode().flip());
    }
}
