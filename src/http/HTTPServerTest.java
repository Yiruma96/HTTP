package http;

import http.core.HTTPHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServerTest {

    public HTTPServerTest() throws Exception{
        ServerSocket serverSocket = new ServerSocket(12345);
        Socket socket = serverSocket.accept();
        System.out.println(socket.getInetAddress().toString() + ":" + socket.getPort());

        HTTPHandler requests = HTTPHandler.readRequests(socket);
        System.out.println(requests.showRequestHead());
        if(requests.hasContent()){
            System.out.println(new String(requests.getContent()));
        }

        HTTPHandler reponse = new HTTPHandler();
        //reponse.setObject(HTTPHandler.object.FILE);
        reponse.setStatusCode("200");
        reponse.setContent("CSTAhahaha".getBytes());
        reponse.setCookie("phpsession", "AvEPjdLfjzW9n8giYgacWuZJgGOHc7");
        //System.out.println(reponse.showReponseHead());
        HTTPHandler.sendReponse(socket, reponse);
        //System.out.println(reponse.showReponseHead());
    }

    public static void main(String[] args) throws Exception{
        new HTTPServerTest();
    }
}
