package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.data.*;

import java.io.IOException;

public class ServerDataTreatmentVisitor implements DataServerVisitor {

    private ServerChatos server;
    private ContextServer context;

    public ServerDataTreatmentVisitor(ServerChatos server){
        this.server = server;
    }

    public void setContext(ContextServer context){
        this.context=context;
    }

    @Override
    public void visit(Login login) throws IOException {
        context.queueMessage(login);
    }

    @Override
    public void visit(OpCode opCode) throws IOException {
        context.queueMessage(opCode);
    }

    @Override
    public void visit(AcceptRequest acceptRequest) throws IOException {
        acceptRequest.setConnect_id(server.definedConnectId(acceptRequest));
        var ctx = acceptRequest.findContextRequester(context);
        ctx.queueMessage(acceptRequest);
        ctx = acceptRequest.findContextTarget(context);
        ctx.queueMessage(acceptRequest);
    }

    @Override
    public void visit(DisconnectRequest disconnectRequest) {
        context.disconnectClient(disconnectRequest.getConnectId());
        var ctx = context.findContextClient(disconnectRequest.getLoginTarget());
        System.out.println(ctx);
        System.out.println(context);
        /*if(ctx != null)
            ctx.queueMessage(new DisconnectRequestConnection(getLoginRequester()));
        else
            logger.info("This client is not connected to the server");
*/
    }

    @Override
    public void visit(DisconnectRequestConnection disconnectRequestConnection) {
        //pas de broadcast
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
        for (ContextServer ctx : server.contextPublic()){
            ctx.queueMessage(messageGlobal);
        }
    }

    @Override
    public void visit(PrivateConnexionTransmission privateConnexionTransmission) throws IOException {
        var keyTarget = server.findKeyTarget(privateConnexionTransmission.getKey());
        ((ContextServer) keyTarget.attachment()).queueMessage(privateConnexionTransmission);
    }

    @Override
    public void visit(PrivateLogin privateLogin) throws IOException {
        context.updatePrivateConnexion(privateLogin.getConnectId(), context.getKey());
        if(!context.connectionReady(privateLogin.getConnectId()))
            return;
        var contexts = context.findContext(privateLogin.getConnectId());
        ((ContextServer) contexts.get(0).attachment()).queueMessage(privateLogin);
        ((ContextServer) contexts.get(1).attachment()).queueMessage(privateLogin);
    }

    @Override
    public void visit(PrivateMessage privateMessage) throws IOException {
        var ctx = context.findContextClient(privateMessage.getLoginTarget());
        ctx.queueMessage(privateMessage);
    }

    @Override
    public void visit(PrivateRequest privateRequest) throws IOException {
        var ctx = privateRequest.findContextTarget(context);
        if(ctx != null)
            ctx.queueMessage(privateRequest);
        else
            System.out.println("This client is not connected");
    }

    @Override
    public void visit(RefuseRequest refuseRequest) throws IOException {
        var ctx = refuseRequest.findContextRequester(context);
        ctx.queueMessage(refuseRequest);
    }
}
