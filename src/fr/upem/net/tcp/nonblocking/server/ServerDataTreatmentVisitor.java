package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.data.*;

import java.io.IOException;

public class ServerDataTreatmentVisitor implements DataServerVisitor {

    private ServerChatos server;

    public ServerDataTreatmentVisitor(ServerChatos server){
        this.server = server;
    }

    @Override
    public void visit(Login login, Context context) throws IOException {
        if(login.processOut(context, server)){
            context.queueMessage(login.encode((byte)1).flip());
        } else {
            context.queueMessage(login.encode((byte)2).flip());
        }
    }

    @Override
    public void visit(OpCode opCode, Context context) throws IOException {
        //pas utile ici
        //context.queueMessage(opCode);
    }

    @Override
    public void visit(AcceptRequest acceptRequest, Context context) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        acceptRequest.setConnect_id(server.definedConnectId(acceptRequest));
        var ctx = acceptRequest.findContextRequester(contextServer);
        ctx.queueMessage(acceptRequest.encode().flip());
        ctx = acceptRequest.findContextTarget(contextServer);
        ctx.queueMessage(acceptRequest.encode().flip());
    }

    @Override
    public void visit(DisconnectRequest disconnectRequest, Context context) {
        ContextServer contextServer = (ContextServer) context;
        contextServer.disconnectClient(disconnectRequest.getConnectId());
        var ctx = server.findContext(disconnectRequest.getLoginTarget());
        System.out.println(ctx);
        System.out.println(context);
        /*if(ctx != null)
            ctx.queueMessage(new DisconnectRequestConnection(getLoginRequester()));
        else
            logger.info("This client is not connected to the server");
*/
    }

    @Override
    public void visit(DisconnectRequestConnection disconnectRequestConnection, Context context) {
        //pas de broadcast
    }

    @Override
    public void visit(HTTPError httpError, Context context) {
        //pas de broadcast
    }

    @Override
    public void visit(HTTPFile httpFile, Context context) {
        //pas de broadcast
    }

    @Override
    public void visit(HTTPRequest httpRequest, Context context) {
        //pas de broadcast
    }

    @Override
    public void visit(MessageGlobal messageGlobal, Context context) throws IOException {
        var bb = messageGlobal.encode();
        if (bb==null) {
            return;
        }
        for (Context ctx : server.contextPublic()){
            ctx.queueMessage(bb.flip());
        }
    }

    @Override
    public void visit(PrivateConnexionTransmission privateConnexionTransmission, Context context) throws IOException {
        var keyTarget = server.findKeyTarget(privateConnexionTransmission.getKey());
        ((ContextServer) keyTarget.attachment()).queueMessage(privateConnexionTransmission.encode());
    }

    @Override
    public void visit(PrivateLogin privateLogin, Context context) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var newContext = contextServer.contextToPrivateContext();
        server.updatePrivateConnexion(privateLogin.getConnectId(), newContext.getKey());
        if(!newContext.connectionReady(privateLogin.getConnectId()))
            return;
        var contexts = server.findContext(privateLogin.getConnectId());
        var response = privateLogin.encodeResponse();
        var response2 = privateLogin.encodeResponse();
        ((Context) contexts.get(0).attachment()).queueMessage(response.flip());
        ((Context) contexts.get(1).attachment()).queueMessage(response2.flip());
        server.printKeys();
        System.out.println("visit private login : ");
        System.out.println("visit private login : ");
    }

    @Override
    public void visit(PrivateMessage privateMessage, Context context) throws IOException {
        var ctx = server.findContext(privateMessage.getLoginTarget());
        ctx.queueMessage(privateMessage.encode().flip());
    }

    @Override
    public void visit(PrivateRequest privateRequest, Context context) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var ctx = privateRequest.findContextTarget(contextServer);
        if(ctx != null)
            ctx.queueMessage(privateRequest.encode().flip());
        else
            System.out.println("This client is not connected");
    }

    @Override
    public void visit(RefuseRequest refuseRequest, Context context) throws IOException {
        ContextServer contextServer = (ContextServer) context;
        var ctx = refuseRequest.findContextRequester(contextServer);
        ctx.queueMessage(refuseRequest.encode().flip());
    }
}
