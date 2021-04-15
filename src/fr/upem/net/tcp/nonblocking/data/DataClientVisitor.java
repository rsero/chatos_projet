package fr.upem.net.tcp.nonblocking.data;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface DataClientVisitor {
    void visit(Login login);
    void visit(OpCode opCode);
    void visit(AcceptRequest acceptRequest) throws IOException;
    void visit(DisconnectRequest disconnectRequest);
    void visit(DisconnectRequestConnection disconnectRequestConnection);
    void visit(HTTPError httpError);
    void visit(HTTPFile httpFile) throws IOException;
    void visit(HTTPRequest httpRequest) throws IOException;
    void visit(MessageGlobal messageGlobal);
    void visit(PrivateConnexionTransmission privateConnexionTransmission);
    void visit(PrivateLogin privateLogin);
    void visit(PrivateMessage privateMessage);
    void visit(PrivateRequest privateRequest) throws IOException;
    void visit(RefuseRequest refuseRequest);
}
