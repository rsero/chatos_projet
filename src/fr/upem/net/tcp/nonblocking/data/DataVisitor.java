package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

public interface DataVisitor {
	/**
	 * Login visitor
	 * @param login
	 * @throws IOException
	 */
    void visit(Login login) throws IOException;
    /**
     * Op code visitor
     * @param opCode
     * @throws IOException
     */
    void visit(OpCode opCode) throws IOException;
    /**
     * Accept Request visitor
     * @param acceptRequest
     * @throws IOException
     */
    void visit(AcceptRequest acceptRequest) throws IOException;
    /**
     * Disconnect request visitor
     * @param disconnectRequest
     */
    void visit(DisconnectRequest disconnectRequest);
    /**
     * Disconnect request Connection visitor
     * @param disconnectRequestConnection
     */
    void visit(DisconnectRequestConnection disconnectRequestConnection);
    /**
     * HTTP error visitor
     * @param httpFile
     * @throws IOException
     */
    void visit(HTTPError httpError);
    /**
     * HTTP file visitor
     * @param httpFile
     * @throws IOException
     */
    void visit(HTTPFile httpFile) throws IOException;
    /**
     * HTTP request visitor
     * @param messageGlobal
     * @throws IOException
     */
    void visit(HTTPRequest httpRequest) throws IOException;
    /**
     * Message global visitor
     * @param messageGlobal
     * @throws IOException
     */
    void visit(MessageGlobal messageGlobal) throws IOException;
    /**
     * Private connection transmission visitor
     * @param privateLogin
     * @throws IOException
     */
    void visit(PrivateConnectionTransmission privateConnectionTransmission) throws IOException;
    /**
     * Private login visitor
     * @param privateLogin
     * @throws IOException
     */
    void visit(PrivateLogin privateLogin) throws IOException;
    /**
     * Private message visitor
     * @param privateRequest
     * @throws IOException
     */
    void visit(PrivateMessage privateMessage) throws IOException;
    /**
     * Private request visitor
     * @param privateRequest
     * @throws IOException
     */
    void visit(PrivateRequest privateRequest) throws IOException;
    /**
     * Refuse request visitor
     * @param refuseRequest
     * @throws IOException
     */
    void visit(RefuseRequest refuseRequest) throws IOException;
}
