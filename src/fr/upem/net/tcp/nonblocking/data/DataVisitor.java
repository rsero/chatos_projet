package fr.upem.net.tcp.nonblocking.data;

import fr.upem.net.tcp.nonblocking.client.Context;

import java.io.IOException;

public interface DataVisitor {
    void visit(Login login) throws IOException;
    void visit(OpCode opCode) throws IOException;
    void visit(AcceptRequest acceptRequest) throws IOException;
    void visit(DisconnectRequest disconnectRequest);
    void visit(DisconnectRequestConnection disconnectRequestConnection);
    void visit(HTTPError httpError);
    void visit(HTTPFile httpFile) throws IOException;
    void visit(HTTPRequest httpRequest) throws IOException;
    void visit(MessageGlobal messageGlobal) throws IOException;
    void visit(PrivateConnexionTransmission privateConnexionTransmission) throws IOException;
    void visit(PrivateLogin privateLogin) throws IOException;
    void visit(PrivateMessage privateMessage) throws IOException;
    void visit(PrivateRequest privateRequest) throws IOException;
    void visit(RefuseRequest refuseRequest) throws IOException;
}