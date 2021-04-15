package fr.upem.net.tcp.nonblocking.data;

import fr.upem.net.tcp.nonblocking.client.Context;

import java.io.IOException;

public interface DataServerVisitor {
    void visit(Login login, Context context) throws IOException;
    void visit(OpCode opCode, Context context) throws IOException;
    void visit(AcceptRequest acceptRequest, Context context) throws IOException;
    void visit(DisconnectRequest disconnectRequest, Context context);
    void visit(DisconnectRequestConnection disconnectRequestConnection, Context context);
    void visit(HTTPError httpError, Context context);
    void visit(HTTPFile httpFile, Context context);
    void visit(HTTPRequest httpRequest, Context context);
    void visit(MessageGlobal messageGlobal, Context context) throws IOException;
    void visit(PrivateConnexionTransmission privateConnexionTransmission, Context context) throws IOException;
    void visit(PrivateLogin privateLogin, Context context) throws IOException;
    void visit(PrivateMessage privateMessage, Context context) throws IOException;
    void visit(PrivateRequest privateRequest, Context context) throws IOException;
    void visit(RefuseRequest refuseRequest, Context context) throws IOException;
}
