package http;

import http.core.HTTPHandler;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;

public class HTTPClientTest2 {

    public HTTPClientTest2() throws Exception {

        //请求上传进度
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

        //请求分析结果
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

        Socket socket = new Socket("127.0.0.1", 12345);

        HTTPHandler request = new HTTPHandler();
        request.setFields("FILE-HASH", new HexBinaryAdapter().marshal(digest));
        request.setMethod(HTTPHandler.method.ANALYSIS);
        request.setObject(HTTPHandler.object.MESSAGE);

        HTTPHandler reponse = HTTPHandler.sendRequestAndReadReponse(socket, request);
        System.out.println(new String(reponse.getContent()));

    }

    public static void main(String[] args) throws Exception {
        new HTTPClientTest2();
    }
}
