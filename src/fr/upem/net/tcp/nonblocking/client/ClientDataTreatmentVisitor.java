package fr.upem.net.tcp.nonblocking.client;

import fr.upem.net.http.server.HTTPServer;
import fr.upem.net.tcp.nonblocking.data.*;

import java.io.*;
import java.nio.charset.Charset;

public class ClientDataTreatmentVisitor implements DataVisitor {

    private final ClientChatos client;
    private final Charset charsetASCII = Charset.forName("ASCII");

    public ClientDataTreatmentVisitor(ClientChatos client){
        this.client=client;
    }

    @Override
    public void visit(Login login) {
        //pas de decode
    }

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
                //client.activePrivateConnection(key);
                break;
            default:
                System.out.println("Operation does not exist");
                break;
        }
    }

    @Override
    public void visit(AcceptRequest acceptRequest) throws IOException {
        Login login;
        if(client.getLogin().equals(acceptRequest.getLoginRequester())){
            login = acceptRequest.getLoginTarget();
        }
        else {
            login = acceptRequest.getLoginRequester();
        }
        System.out.println("targetlogin after acceptrequest is : "+ login);
        client.addConnect_id(acceptRequest.getConnect_id(), login);
        System.out.println("Connection " + acceptRequest.loginRequester() + " : " + acceptRequest.loginTarget() + " is established with id : "+ acceptRequest.getConnect_id()
                + "\n \"/id "+ acceptRequest.getConnect_id() +"\" to accept");
    }

    @Override
    public void visit(DisconnectRequest disconnectRequest) {
        //pas de decode
    }

    @Override
    public void visit(DisconnectRequestConnection disconnectRequestConnection) {
        client.deleteRequestConnection(disconnectRequestConnection.getLogin());
    }

    @Override
    public void visit(HTTPError httpError) {
        System.out.println("The file " + httpError.getFile() + " was not found");
    }

    @Override
    public void visit(HTTPFile httpFile) throws IOException {
        var buffRead = httpFile.getBuffRead();
        if (httpFile.isTextFile()) {
            buffRead.flip();
            System.out.println("File received : \n" + charsetASCII.decode(buffRead).toString());
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

    @Override
    public void visit(HTTPRequest httpRequest) throws IOException {
        new HTTPServer(httpRequest.getFile(), httpRequest.getKey(), client.getDirectory()).serve();
    }

    @Override
    public void visit(MessageGlobal messageGlobal) {
        if(client.isConnected())
            System.out.println(messageGlobal.getLogin() + " : " + messageGlobal.getMsg());
    }

    @Override
    public void visit(PrivateConnexionTransmission privateConnexionTransmission) {
        //pas de decode
    }

    @Override
    public void visit(PrivateLogin privateLogin) {
        //pas de decode
    }

    @Override
    public void visit(PrivateMessage privateMessage) {
        System.out.println(privateMessage.getLoginSender() + " : " + privateMessage.getMsg());
    }

    @Override
    public void visit(PrivateRequest privateRequest) throws IOException {
        client.addSetPrivateRequest(privateRequest);
        client.addConnection(new Login(privateRequest.loginRequester()));
        System.out.println(privateRequest);
    }

    @Override
    public void visit(RefuseRequest refuseRequest) {
        refuseRequest.deleteRequestConnection(client);
        System.out.println(refuseRequest);
    }
}
