package http;

import http.core.HTTPHandler;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;

import static http.core.HTTPHandler.method.ANALYSIS;

public class HTTPClientTest {

    public HTTPClientTest() throws Exception {
//        Socket socket = new Socket("127.0.0.1", 12345);
//
//        HTTPHandler request = new HTTPHandler();
//        request.setObject(HTTPHandler.object.FILE);
//        request.setMethod(HTTPHandler.method.TEST);
//        request.setFields("test", "test");
////        request.setContent("jisuanjikejixiehui".getBytes());
//        HashMap<String, String> POST = new HashMap<String, String>();
//        POST.put("username","yiruma");
//        POST.put("password","2333");
//        request.addPOST(POST);
//        request.setCookie("aaa","aaa");
//        System.out.println(request.showRequestHead());
//
//        HTTPHandler reponse = HTTPHandler.sendRequestAndReadReponse(socket, request);
//        System.out.println(reponse.showReponseHead());
//        if(reponse.hasContent()){
//            System.out.println(new String(reponse.getContent()));
//        }


//登陆
//        Socket socket = null;
//        try {
//            socket = new Socket("127.0.0.1",12345);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        HTTPHandler request = new HTTPHandler();
//        request.setObject(HTTPHandler.object.MESSAGE);
//        request.setMethod(HTTPHandler.method.LOGIN);
//        HashMap<String,String> POST = new HashMap<>();
//        POST.put("username","liujun");
//        POST.put("password","123456");
//        request.addPOST(POST);
//        try {
//            HTTPHandler response = HTTPHandler.sendRequestAndReadReponse(socket,request);
//            System.out.println(response.showReponseHead());
//            System.out.println(new String(response.getContent()));
//            System.out.println(response.getCookie("session"));
//            socket.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//文件发送
        String filePath = "E:/jboh.apk";

        //文件输入流与md吸收流的建立
        FileInputStream fis = null;
        byte[] digest = null;
        StringBuilder result = new StringBuilder();
        try {
            fis = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //整合为数据流Filter
            DigestInputStream dis = new DigestInputStream(fis, md);
            //进行过滤
            byte[] buffer = new byte[4096];
            while(dis.read(buffer) != -1);
            digest = md.digest();
            dis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }



        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1" ,12345);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HTTPHandler request = new HTTPHandler();
        request.setObject(HTTPHandler.object.FILE);
        request.setMethod(HTTPHandler.method.UPLOAD);
        request.setFields("File-Size", String.valueOf(new File(filePath).length()));
        request.setFields("File-Hash", new HexBinaryAdapter().marshal(digest));

        HTTPHandler response = null;
        try {
            response = HTTPHandler.sendRequestAndReadReponse(socket,request);
            socket.close();
            System.out.println(response.showReponseHead());
            System.out.println(new String(response.getContent()));
        } catch (Exception e) {
            socket.close();
            e.printStackTrace();
        }

        //解析出基本数据
        int openPort;
        int start;
        int end;
        int length;
        if(response.getStatusCode().equals("200")) {

            openPort = Integer.valueOf(response.getFields("Open-Port"));
            String contentRange = response.getFields("Content-Range");

            start = Integer.valueOf(contentRange);

            System.out.println(start);

            //开始传输
            socket = new Socket("127.0.0.1", openPort);
            RandomAccessFile oSavedFile = new RandomAccessFile(filePath, "r");
            oSavedFile.seek(start);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int currentSize = start;
            byte[] buffer = new byte[1024];
            int read = 0;
            while(true){
                Thread.sleep(0);
                read = oSavedFile.read(buffer);

                if(read != -1){
                    dos.write(buffer, 0, read);
                    currentSize += read;
                    System.out.println(currentSize);
                }else{
                    System.out.println(currentSize);
                    socket.close();
                    break;
                }
            }

            socket.close();
        }

        socket.close();


//进度反馈
//        Socket socket = new Socket("127.0.0.1", 12345);
//
//        HTTPHandler request = new HTTPHandler();
//        request.setObject(HTTPHandler.object.MESSAGE);
//        request.setMethod(HTTPHandler.method.PROCESS);
//        String fileHash1 = "1DE2823BDB0C673486BAC9D27DCAAB38474256865074E501403C470AA75AD04D";
//        String fileHash2 = "0000000000000000000000000000000000000000000000000000000000000000";
//        request.setContent((fileHash1+"&"+fileHash2).getBytes());
//
//        HTTPHandler reponse = HTTPHandler.sendRequestAndReadReponse(socket, request);
//        System.out.println(reponse.showReponseHead());
//        if(reponse.hasContent()){
//            String content = new String(reponse.getContent());
//            HashMap<String, String> fileSize = HTTPHandler.paraStrToHashMap(content);
//            System.out.println(fileSize);
//        }


        // 请求分析结果
//        Socket socket = new Socket("127.0.0.1", 12345);
//
//        HTTPHandler request = new HTTPHandler();
//        request.setFields("FILE-HASH", new HexBinaryAdapter().marshal(digest));
//        request.setMethod(HTTPHandler.method.ANALYSIS);
//        request.setObject(HTTPHandler.object.MESSAGE);
//
//        HTTPHandler reponse = HTTPHandler.sendRequestAndReadReponse(socket, request);
//        System.out.println(new String(reponse.getContent()));


    }

    public static void main(String[] args) throws Exception {
        new HTTPClientTest();
    }
}
