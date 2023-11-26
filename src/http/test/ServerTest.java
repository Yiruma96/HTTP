package http.test;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerTest {

    //直接将接收写成一个线程类就可以了，接收到socket后直接开启此类
    public static void main(String[] args) throws Exception {

        int size = 0;
        try{
            ServerSocket serverSocket = new ServerSocket(12346);
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println(socket.getInetAddress().toString()+socket.getPort());

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                FileOutputStream fis = new FileOutputStream("E:/test");

                byte[] buffer = new byte[1024];
                int read = 0;
                while(true){
                    read = 0;
                    //System.out.println("1");
                    read = dis.read(buffer);
                    //System.out.println("2");
                    if(read != -1){
                        //System.out.println(new String(buffer));
                        fis.write(buffer, 0, read);
                        size += read;
                        fis.flush();
                    }else{
                        socket.sendUrgentData(1);
                    }
                    System.out.println(read);
                }
            }
        }catch (java.net.SocketException e){
            System.out.println("客户端已经关闭连接，此时已经接收了"+size+"字节");
        }



    }
}
