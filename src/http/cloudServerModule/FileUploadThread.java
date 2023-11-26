package http.cloudServerModule;

import adstatic.Start;
import http.core.HTTPHandler;
import http.libs.DBUnit;
import http.libs.HandleRetu;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

import soot.G;


public class FileUploadThread implements Runnable {

    private HTTPHandler requestHead;

    private String serverSocketPort;
    private Socket socket;
    private ServerSocket fileUploadServerSocket;
    private String clientServerIP = "";
    private int clientServerPort = 12345;
    private static int SOCKET_READ_TIME_OUT = 5000;  //socket读的超时时间设置为5s

    private String fileHash;
    private int fileSize;
    private int start;

    //两个size要尽量保持同步
    private ConcurrentHashMap<String, Integer> currentSizeMap;  //这个size用于向线程外反馈进度，需要反馈的时候从currentSize更新下即可
    private int currentSize;                                    //这个size时刻保持最新变化

    //private String[] blackList = new String[]{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"};
    private String filePath = "uploadAPK/";
    private String analysisPath = "analysisRetu/";
    private int FILE_UPLOADED_MAX_SIZE = 1024*1024*200;

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet rs = null;

    private class RateOfProcessFeedThread implements Runnable {

        @Override
        public void run() {

//            int lastSendProcess = currentSize;

            while (true){

                // 基于时间进度发送反馈请求
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                sendRateOfProcess();

//                // 基于接收进度发送反馈请求
//                // 在上传进度超过了0.01时，发送一次请求
//                if(((currentSize - lastSendProcess) / (float)fileSize) > 0.01){
//                    //在发送之前置为0的原因因为进度反馈那里使用多线程做的，可能在还没有发送完第二次又到了
//                    //也就是不管是否发送成功，都不再进行第二次进度反馈了
//                    lastSendProcess = currentSize;
//                    sendRateOfProcess();
//                }
            }
        }

        private void sendRateOfProcess() {

             Socket rateFeedBackSocket = null;
             try {
                 rateFeedBackSocket = new Socket(clientServerIP, clientServerPort);
                 HTTPHandler requestRateOfProcess = new HTTPHandler();
                 requestRateOfProcess.setObject(HTTPHandler.object.MESSAGE);
                 //requestRateOfProcess.setMethod(HTTPHandler.method.PROCCESSRATE);
                 requestRateOfProcess.setFields("File-Hash", fileHash);
                 //requestRateOfProcess.setFields("Current-Size", String.valueOf(currentSize));
                 HTTPHandler.sendRequest(rateFeedBackSocket, requestRateOfProcess);
             } catch (Exception e) {
                 System.out.println(e);  //打印错误但并不做处理
             } finally {
                 if(rateFeedBackSocket != null){
                     try {
                         rateFeedBackSocket.close();
                     } catch (IOException e) {
                         System.out.println(e);  //同样不做处理
                     }
                 }
             }
        }
    }


    public FileUploadThread(Socket socket, HTTPHandler requestHead, ConcurrentHashMap<String, Integer> currentSizeMap){
        this.requestHead = requestHead;
        this.socket = socket;
        this.currentSizeMap = currentSizeMap;

        try {
            conn = DBUnit.getConnection();
        }catch(Exception e){
            System.out.println("数据库连接失败");
        }
    }

    private boolean fileNameFilter(String fileName){

        if(fileName == null){
            return false;
        }

//        //文件名中不能含有黑名单中的字符
//        for (String blackChar : blackList) {
//            if(fileName.contains(blackChar)){
//                return false;
//            }
//        }

        //文件名结尾为.apk
        if(!fileName.endsWith(".apk")){
            return false;
        }

        //将文件名于本地存储的映射添加到数据库中，为username, uploadFileName, FileName
        //目前先实现文件同名存储

        //均通过检测返回可用
        return true;
    }

    private HandleRetu headParse(){

        HandleRetu handleRetu = new HandleRetu(true, "parse success");

        // 对File-Size的解析，检查文件大小是否超过限制
        if(handleRetu.getFlag()){
            if(requestHead.getFields("File-Size") == null) {
                handleRetu.setFlag(false);           //缺失File-Size字段
                handleRetu.setDesc("File-Size is not exist");
            }else{

                try{
                    this.fileSize = Integer.valueOf(requestHead.getFields("File-Size"));
                    if(this.fileSize > FILE_UPLOADED_MAX_SIZE){
                        handleRetu.setFlag(false);   //文件上传大小超过限制
                        handleRetu.setDesc("file size out of limit");
                    }
                }catch (java.lang.NumberFormatException e){
                    handleRetu.setFlag(false);       //File-Size格式异常，并不是纯数字
                    handleRetu.setDesc("File-Size format is wrong");
                }
            }
        }

        //对File-Hash的解析  检查数据库中是否已经存在文件
        if(handleRetu.getFlag()) {
            this.fileHash = requestHead.getFields("File-Hash");

            if(fileHash == null){
                handleRetu.setFlag(false);           //缺失File-Hash
                handleRetu.setDesc("File-Hash is not exist");
            }else{

                //检查该hash是否存在
                try{
                    preparedStatement = conn.prepareStatement("select * from apk where apk_hash=?");
                    preparedStatement.setString(1, this.fileHash);
                    rs = preparedStatement.executeQuery();
                    if(rs.next()){
                        handleRetu.setFlag(false);
                        handleRetu.setDesc("same file exist");
                    }
                }catch(Exception e){
                    System.out.println("连接出现问题");
                    handleRetu.setFlag(false);
                    handleRetu.setDesc("error");
                }
            }
        }

        //前面执行均没有问题的话，从数据库中提取出来文件上传的进度
        if(handleRetu.getFlag()) {
            try {
                preparedStatement = conn.prepareStatement("select start from upload_process where apk_hash=?");
                preparedStatement.setString(1, this.fileHash);
                rs = preparedStatement.executeQuery();
                if(rs.next()){
                    //找到上传记录，拿到开始传输的位置
                    this.start = Integer.valueOf(rs.getString("start"));
                }else{
                    //数据库中并没有关于此文件的上传记录，将上传开始位置置为0，并插入到数据库中
                    preparedStatement = conn.prepareStatement("insert upload_process values(?,?)");
                    preparedStatement.setString(1, fileHash);
                    preparedStatement.setString(2, "0");
                    preparedStatement.executeUpdate();
                    this.start = 0;
                }
            } catch (Exception e) {
                System.out.println("连接出现问题");
                handleRetu.setFlag(false);
                handleRetu.setDesc("error");
            }
        }

        //尝试开启serverSocket
        if(handleRetu.getFlag()){
            try {
                this.fileUploadServerSocket = new ServerSocket(0);  //端口指定为0表示使用随机的未被占用的端口
                this.serverSocketPort = String.valueOf(this.fileUploadServerSocket.getLocalPort());
            } catch (Exception e) {
                handleRetu.setFlag(false);   //serverSocket的生成出现异常
                handleRetu.setDesc("upload serverSocket init error");
            }
        }

        //到此为止，对head的提取工作已经结束了，如果存在问题，就返回false
        return handleRetu;

    }

    public int getCurrentSize(){
        return currentSize;
    }


    private void uploadStart() throws Exception {

        this.currentSize = start;

//        int lastSendProcess = start;
        DataInputStream dis = null;
        RandomAccessFile oSavedFile = null;
        Socket fileUploadSocket = null;
        try{
            fileUploadSocket = this.fileUploadServerSocket.accept();
            this.fileUploadServerSocket.close();
            fileUploadSocket.setSoTimeout(SOCKET_READ_TIME_OUT);
            System.out.println(fileUploadSocket.getInetAddress().toString()+":"+fileUploadSocket.getPort());

            oSavedFile = new RandomAccessFile(filePath+this.fileHash+".apk", "rw");
            oSavedFile.seek(this.start);  //根据传过来的协议进行随机文件读写的定位
            dis = new DataInputStream(fileUploadSocket.getInputStream());
            OutputStream os = fileUploadSocket.getOutputStream();

            byte[] buffer = new byte[1024];
            int read = 0;
            while(true){

                //每一轮接收，就更新一下fileSize的值
                currentSizeMap.put(fileHash, currentSize);

                if(currentSize > FILE_UPLOADED_MAX_SIZE){
                    break;    // 文件超过大小限制
                }

                read = dis.read(buffer);
                if(read != -1){
                    //System.out.println(new String(buffer));
                    oSavedFile.write(buffer, 0, read);
                    currentSize += read;
                }else{
                    //在没有接收到数据的情况下，考虑是不是对面已经关闭socket结束发送了，这也是while的一种跳出条件
                    fileUploadSocket.sendUrgentData(1);
                }

                if(currentSize == fileSize) {  //在接收完毕后跳出，要么就一直等着接收，直到读超时或者对方socket关闭
                    dis.close();

                    currentSizeMap.put(fileHash, currentSize);  //传输结束的时候再更新一下
                    break;
                }
            }
            oSavedFile.close();
            dis.close();
            fileUploadSocket.close();
        }catch (java.net.SocketException e){
            //System.out.println(e);
            if(dis != null){
                dis.close();
            }
            if(oSavedFile != null){
                oSavedFile.close();
            }
            if(fileUploadSocket != null) {
                fileUploadSocket.close();
            }
            System.out.println("客户端已经关闭连接，此时已经接收了"+currentSize+"字节");
        }


        //对上面的传输结果进行处理
        if(currentSize == fileSize){
            //文件上传完成，需要录入到apk表中,同时删除upload_process中的记录
            System.out.println("apk upload finish");
            try{
                preparedStatement = conn.prepareStatement("insert apk values(?,?,?)");
                preparedStatement.setString(1, this.fileHash);
                preparedStatement.setString(2, String.valueOf(fileSize));
                preparedStatement.setBoolean(3, false);
                preparedStatement.executeUpdate();

                preparedStatement = conn.prepareStatement("delete from upload_process where apk_hash=?");
                preparedStatement.setString(1, this.fileHash);
                preparedStatement.executeUpdate();
            }catch (Exception e){
                System.out.println("连接出现问题");
                //System.out.println(e);
            }

        } else if(currentSize < fileSize){
            //文件上传还没有完成，及时存到数据库中
            try{
                preparedStatement = conn.prepareStatement("update upload_process set start=? where apk_hash=?");
                preparedStatement.setString(1, String.valueOf(currentSize));
                preparedStatement.setString(2, this.fileHash);
                preparedStatement.executeUpdate();
            }catch(Exception e){
                System.out.println("连接出现问题");
                //System.out.println(e);
            }
        }


    }


    @Override
    public void run(){

        HTTPHandler response = new HTTPHandler();

        HandleRetu handleRetu= headParse();

        System.out.println("开始上传的位置为"+start);

        if(handleRetu.getFlag()){
            response.setStatusCode("200");
            response.setFields("Open-Port", this.serverSocketPort);
            response.setFields("Content-Range", String.valueOf(start));
        }else{
            //head解析出现错误，返回异常
            response.setStatusCode("409");
            response.setContent(handleRetu.getDesc().getBytes());
        }

        //关闭请求socket
        try {
            HTTPHandler.sendReponse(this.socket, response);
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
            //e.printStackTrace();
        }

        // deprecated this func 客户端采用轮询方式
        //开启文件上传进度反馈进程，在文件上传线程结束时，此线程应该也结束才对
        //new Thread(new RateOfProcessFeedThread()).start();

        //解析正确，开始尝试文件传输
        if(handleRetu.getFlag()) {
            try {
                uploadStart();
                System.out.println("uploadstart结束");
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println(e);
            }
        }

        DBUnit.release(conn, preparedStatement, rs);
    }



}
