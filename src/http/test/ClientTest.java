package http.test;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

//https://www.ibm.com/developerworks/cn/java/joy-down/
public class ClientTest {

    public static void main(String[] args) throws Exception{
        Socket socket = new Socket("127.0.0.1", 12346);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(new FileInputStream("E:/test.apk"));

        byte[] buffer = new byte[1024];
        while(true){
            int read = 0;
            read = dis.read(buffer);
            if(read != -1){
                dos.write(buffer, 0, read);
            }else{
                socket.close();
                break;
            }
        }

        socket.close();

    }

}
