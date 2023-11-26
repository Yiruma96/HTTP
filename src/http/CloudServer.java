package http;

import Server.ServerMain;
import adstatic.tools.MyTools;
import http.cloudServerModule.FileUploadThread;
import http.cloudServerModule.LoginThread;
import http.cloudServerModule.ObtainAnalysisResult;
import http.cloudServerModule.RegisterThread;
import http.core.HTTPHandler;
import http.libs.AnalysisThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudServer {

    private ServerSocket serverSocket;
    private int port;

    private ExecutorService threadPoolSocketHandler;
    private int poolSize;  //后期需要根据情况调整，另外观察超过线程时的反应情况
    private ConcurrentHashMap<String, Integer> currentSizeMap;  //用于记录一些线程外需要访问的数据
    //使用默认参数的构造函数
    public CloudServer(){

        //读取配置文件
        Map<String, String> serverConfigMap = MyTools.getConfig("SERVERSOCKET");

        try {
            port = Integer.valueOf(serverConfigMap.get("PORT"));
            poolSize = Integer.valueOf(serverConfigMap.get("POOLSIZE"));
            serverSocket = new ServerSocket(port);
            System.out.println("ServerSocket打开");
        } catch (Exception e) {
            System.out.println("ServerSocket打开异常");
            e.printStackTrace();
        }

        threadPoolSocketHandler = Executors.newFixedThreadPool(poolSize);
        currentSizeMap = new ConcurrentHashMap<String, Integer>();

    }

    public CloudServer(int port, int poolSize) throws Exception {
        this.port = port;
        this.poolSize = poolSize;

        try {
            serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            System.out.println("ServerSocket打开异常");
            e.printStackTrace();
        }

        threadPoolSocketHandler = Executors.newFixedThreadPool(this.poolSize);
    }


    public void start(){

        // 开启文件文件分析线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new AnalysisThread().analysisStart();
                } catch (Exception e) {
                    System.out.println(e);
                    //e.printStackTrace();
                }
            }
        }).start();

        // 开启ServerSocket接受请求
        Socket socket;
        while(true){

            try {
                socket = serverSocket.accept();
                String ip = socket.getInetAddress().toString();
                String port = String.valueOf(socket.getPort());
                System.out.println("连接到" + ip + ":" + port);
            } catch (IOException e) {
                break;
            }

            try {
                HTTPHandler request = HTTPHandler.readHttpHead(socket, HTTPHandler.headType.REQUEST);
                System.out.println(request.showRequestHead());
                modelRouter(request, socket);
            } catch (Exception e) {
                String ip = socket.getInetAddress().toString();
                String port = String.valueOf(socket.getPort());
                System.out.println("与" + ip + ":" + port + "的通信出现异常");
                break;
            }

        }
    }


    private void modelRouter(HTTPHandler requestHead, Socket socket){

        //文件相关
        if(requestHead.getObject() == HTTPHandler.object.FILE){

            //***android端请求文件上传
            if(requestHead.getMethod() == HTTPHandler.method.UPLOAD){
                if (requestHead.getFields("File-Hash") != null)
                    threadPoolSocketHandler.submit(new FileUploadThread(socket, requestHead, currentSizeMap));
                    return;
            }


        }


        //字符消息相关
        if(requestHead.getObject() == HTTPHandler.object.MESSAGE){

            //将字符全部读取出来
            String content = "";
            String contentLengthStr = requestHead.getFields("Content-Length");
            if(contentLengthStr != null){ // 没有Content-Length字段的话就不读取消息体了
                try {
                    int contentLength = Integer.valueOf(contentLengthStr); //可能存在转换异常
                    content = new String(HTTPHandler.readHttpContent(socket, contentLength));
                }catch(Exception e){}
            }

            System.out.println(content);
            //System.out.println(requestHead.getMethod());

            //***Android端请求登陆
            if(requestHead.getMethod() == HTTPHandler.method.LOGIN){
                threadPoolSocketHandler.submit(new LoginThread(socket, content));
                return;
            }

            //***Android端请求注册
            if(requestHead.getMethod() == HTTPHandler.method.REGISTER){
                threadPoolSocketHandler.submit(new RegisterThread(socket, content));
                return;
            }

            //***Android端请求进度反馈
            if(requestHead.getMethod() == HTTPHandler.method.PROCESS){

                HashMap<String, String> processMap = new HashMap<>();
                Integer currentSize;
                for (String fileHash : content.split("&")) {
                    currentSize = currentSizeMap.get(fileHash);
                    System.out.println(currentSize);
                    processMap.put(fileHash, String.valueOf(currentSize == null? 0 : currentSize));
                }

                HTTPHandler response = new HTTPHandler();
                response.setStatusCode("200");
                response.addPOST(processMap);
                try {
                    HTTPHandler.sendReponse(socket, response);
                    return;
                } catch (Exception e) {
                    //pass 出现异常的话，顺序往下走，会发送410的错误
                }
            }

            //***Android端请求分析结果
            if(requestHead.getMethod() == HTTPHandler.method.ANALYSIS){
                threadPoolSocketHandler.submit(new ObtainAnalysisResult(socket, requestHead));
                return;
            }
        }


        //走到这里的都是没有被解析的，所以这里需要进行4xx的反馈，表示发送了服务器无法理解的HTTP头，之后关闭socket
        HTTPHandler reponse = new HTTPHandler();
        reponse.setStatusCode("410");
        try {
            HTTPHandler.sendReponse(socket, reponse);
        } catch (Exception e) {
            String ip = socket.getInetAddress().toString();
            String port = String.valueOf(socket.getPort());
            System.out.println("与" + ip + ":" + port + "的通信出现异常");
        }

        try {
            socket.close();
        } catch (IOException e) {
            //不做处理
        }

    }


    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new CloudServer().start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                new ServerMain().start();
            }
        }).start();


    }




}
