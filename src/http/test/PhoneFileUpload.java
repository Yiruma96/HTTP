package http.test;

import http.core.HTTPHandler;

import java.io.File;
import java.net.Socket;

public class PhoneFileUpload {

    String serverIP = "127.0.0.1";
    int serverPort = 12345;

    String fileName;
    int fileSize;
    int start;
    int end;

    public PhoneFileUpload(String fileName){
        this.fileName = fileName;
    }

    public void fileStartUpload() throws Exception {

        File file = new File(fileName);
        if(!file.isFile()){ //检测指定文件是否存在 + 是否是文件
            throw new Exception("file not exist!");
        }

        this.fileSize = (int) file.length();

        //首先构建一个http协议向服务器请求，来获得需要的
        HTTPHandler httpHandler = new HTTPHandler();
        httpHandler.setObject(HTTPHandler.object.FILE);
        httpHandler.setFields("File-Name", fileName);
        httpHandler.setFields("Content-Length", String.valueOf(fileSize));

        Socket fileUploadSocket = new Socket("10.8.44.103", 12345);
        HTTPHandler.sendRequestAndReadReponse(fileUploadSocket, httpHandler);
    }

    public static void main(String[] args) throws Exception {
        PhoneFileUpload phoneFileUpload = new PhoneFileUpload("F:/apk/up1");
//        phoneFileUpload.fileStartUpload();

    }

}
