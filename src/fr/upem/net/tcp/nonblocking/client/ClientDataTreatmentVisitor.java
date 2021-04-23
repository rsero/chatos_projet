package fr.upem.net.tcp.nonblocking.client;

import fr.upem.net.http.server.HTTPServer;
import fr.upem.net.tcp.nonblocking.data.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ClientDataTreatmentVisitor implements DataVisitor {

    private final ClientChatos client;

    public ClientDataTreatmentVisitor(ClientChatos client){
        this.client=client;
    }

    /**
     * The visitor does nothing when a login object is passed
     * @param login login received
     */
    @Override
    public void visit(Login login) {
        //do nothing
    }

    /**
     * Analyses the opCode value passed and updates the client according
     * @param opCode the opcode returned
     */
    @Override
    public void visit(OpCode opCode) {
        switch (opCode.getByte()) {
            case 1:
                client.updateLogin();
                System.out.println("Identification accepted");
                break;
            case 2:
                System.out.println("Login already taken");
                break;
            case 10:
                System.out.println("Connexion was established");
                client.activePrivateConnection(opCode.getKey());
                client.privateConnection();
                break;
            default:
                System.out.println("Operation does not exist");
                break;
        }
    }

    /**
     * Prints a message on both clients sides to give them the connectId created by the server
     * Adds the client to the list of established private connection list
     * @param acceptRequest accept request notification received
     * @throws IOException if the connection is closed
     */
    @Override
    public void visit(AcceptRequest acceptRequest) throws IOException {
        Login login;
        if(client.getLogin().equals(acceptRequest.getLoginRequester())){
            login = acceptRequest.getLoginTarget();
        }
        else {
            login = acceptRequest.getLoginRequester();
        }
        client.addConnect_id(acceptRequest.getConnect_id(), login);
        System.out.println("Connection " + acceptRequest.loginRequester() + " : " + acceptRequest.loginTarget() + " is established with id : "+ acceptRequest.getConnect_id()
                + "\n \"/id "+ acceptRequest.getConnect_id() +"\" to accept");
    }

    /**
     * The visitor does nothing when a Disconnect request object is passed
     * @param disconnectRequest disconnect request notification received
     */
    @Override
    public void visit(DisconnectRequest disconnectRequest) {
        //do nothing
    }

    /**
     * Removes the client from the list of established private connections
     * @param disconnectRequestConnection disconnect request notification received
     */
    @Override
    public void visit(DisconnectRequestConnection disconnectRequestConnection) {
        client.deleteRequestConnection(disconnectRequestConnection.getLogin());
    }

    /**
     * Prints a message saying the file was not found
     * @param httpError error notification received
     */
    @Override
    public void visit(HTTPError httpError) {
        System.out.println("The file " + httpError.getFile() + " was not found");
    }

    /**
     * Processes the file received :
     * - if the file is a text file, prints it
     * - else saves it in the directory given in the command line
     * @param httpFile http file received
     * @throws IOException if the file could not be written
     */
    @Override
    public void visit(HTTPFile httpFile) throws IOException {
        var buffRead = httpFile.getBuffRead();
        if (httpFile.isTextFile()) {
            buffRead.flip();
            System.out.println("File received : \n" + StandardCharsets.UTF_8.decode(buffRead).toString());
        } else {
            var shortPath = client.getDirectory()+"/"+ httpFile.getNameFile();
            var path = new File(shortPath).toURI().getPath();
            File initialFile = new File(path);
            buffRead.flip();

            OutputStream outputStream = new FileOutputStream(initialFile);

            byte[] arr = buffRead.array();
            outputStream.write(arr);
            outputStream.close();
        }
    }

    /**
     * Processes a file request by starting an HTTP Server
     * @param httpRequest http request received
     * @throws IOException if the connection was lost
     */
    @Override
    public void visit(HTTPRequest httpRequest) throws IOException {
        new HTTPServer(httpRequest.getFile(), httpRequest.getKey(), client.getDirectory());
    }

    /**
     * Processes a global message object :
     * - prints the message and its sender for all the clients identified
     * @param messageGlobal global message received
     */
    @Override
    public void visit(MessageGlobal messageGlobal) {
        if(client.isConnected())
            System.out.println(messageGlobal.getLogin() + " : " + messageGlobal.getMsg());
    }

    /**
     * The visitor does nothing when a privateConnectionTransmission object is passed
     * @param privateConnectionTransmission private connection object received
     */
    @Override
    public void visit(PrivateConnectionTransmission privateConnectionTransmission) {
        //do nothing
    }

    /**
     * The visitor does nothing when a privateLogin object is passed
     * @param privateLogin private login object received
     */
    @Override
    public void visit(PrivateLogin privateLogin) {
        //pas de decode
    }

    /**
     * Prints the private message received from another client
     * @param privateMessage private message sent by another client
     */
    @Override
    public void visit(PrivateMessage privateMessage) {
        System.out.println(privateMessage.getLoginSender() + " : " + privateMessage.getMsg());
    }

    /**
     * Adds a private request to the client and notifies that a request was received
     * @param privateRequest request for a private connection sent by another client
     */
    @Override
    public void visit(PrivateRequest privateRequest) {
        client.addSetPrivateRequest(privateRequest);
        System.out.println(privateRequest);
    }

    /**
     * Removes the private client of the private request list and notifies that the request was refused
     * @param refuseRequest refuse response for a private connection sent by another client
     */
    @Override
    public void visit(RefuseRequest refuseRequest) {
        refuseRequest.deleteRequestConnection(client);
        System.out.println(refuseRequest);
    }
}
