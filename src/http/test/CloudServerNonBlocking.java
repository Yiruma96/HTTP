package http.test;

import http.core.HTTPHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*这是服务端服务器的主接口，使用ServerSocket来开放指定端口来监听所有客户端的链接，
使用线程池来容纳所有的来访请求，在ServerSocket监听到一个客户端的来访请求时，就在线程池中开启一个线程来处理这个连接请求，
所有对链接请求的获取http头，然后解析并转到相应的处理模块由这个线程来完成，这里最好维护一个链接的通信表，因为文件传输那个问题还没有解决，能用抽象类线程实现吗？？
感觉只能用非阻塞与多线程综合实现了，在本类中接收完所有的http，然后创建线程去处理，嗯，只能这样了，记得维护一个线程通信列表方便通知*/
public class CloudServerNonBlocking {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private int port = 12345;
    private Charset charset = Charset.forName("GBK");
    private int HTTP_HEADER_LENGTH_MAX = 1024;  //限制接收中的长度，如果超过此长度的话会返回错误

    private ExecutorService threadPoolSocketHandler;
    private int POOL_SIZE = 2;  //后期需要根据情况调整，另外观察超过线程时的反应情况

    private ServerSocket fileUploadServerSocket;  //用于文件上传的serverSocket
    private int fileUplaodPort = 12346;  //指定用于文件上传的端口

    private HTTPHandler httpHandler = new HTTPHandler();

    public CloudServerNonBlocking() throws Exception {

        serverSocketChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverSocketChannel.configureBlocking(false);   //serverSocketChannel工作在非阻塞模式

        threadPoolSocketHandler = Executors.newFixedThreadPool(POOL_SIZE);  //初始化线程池用于分析http后进行下一步的处理

        fileUploadServerSocket = new ServerSocket(fileUplaodPort);
    }

    public void Service() throws Exception{
        //serverSocketChannel向selector中注册OP_ACCEPT事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //selector循环产生事件通知，当然每次通知不一定只产生一个事件
        while(selector.select() > 0){

            //获得此次事件通知下所有的就绪事件
            Set<SelectionKey> readykeys = selector.selectedKeys();
            Iterator<SelectionKey> it = readykeys.iterator();

            //开始循环解决所有的就绪事件
            while(it.hasNext()){
                //将一个事件从selector的selectKeys中取出并删除
                SelectionKey key = it.next();
                it.remove();

                //处理连接事件
                if(key.isAcceptable()){
                    accept(key);
                }

                //只需要处理Socket的读事件，socket的写在线程内部完成
                if(key.isReadable()){
                    read(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws Exception {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel(); // 从此次要解决的事件中拿到ServerSocketChannel
        SocketChannel socketChannel = ssc.accept();  // 从此serverSocketChannel中获得请求连接的accept

        socketChannel.configureBlocking(false);  // socketChannel需要设置为非阻塞模
        ByteBuffer buffer = ByteBuffer.allocate(HTTP_HEADER_LENGTH_MAX);
        socketChannel.register(selector, SelectionKey.OP_READ, buffer); //socketChannel向selector中注册OP_READ事件，并添加一个有长度限制的buffer用于接收数据

    }

    private void read(SelectionKey key) throws Exception {
        //第一部分是个统一的接收http头的部分
        SocketChannel socketChannel = (SocketChannel) key.channel(); // 从此次要解决的事件中拿到SocketChannel
        Socket socket = socketChannel.socket();
        ByteBuffer buffer = (ByteBuffer)key.attachment(); // 从key中取得socket绑定的channel
        byte[] single = new byte[1];

        //按照书上例子，每次读取32个字节，所以一共需要32次循环
        String httpHeader;
        for(int i = 0; i<32 ; i++){
            if(socket.getInputStream().read(single) != 0){ //只有在的确收到一个byte的时候才进行处理，Fix it，考虑这里的异常情况，如果一直接收不到怎么办？
                //从socket中读一字节的数据
                if(buffer.position() == HTTP_HEADER_LENGTH_MAX){
                    //Fix it 抛出头部溢出异常，进行处理
                }
                buffer.put(single);

                //每接收到一字节后，就对当前收集到的http头进行检测，避免接收到了主体部分的数据
                buffer.flip();
                httpHeader = charset.decode(buffer).toString();
                if(httpHeader.contains("\r\n\r\n")){ //http头部结束的判定条件————存在两个连续的\r\n
                    HTTPHandler httpHandler = null;//HTTPHandler.httpHeaderParser(httpHeader);
                    if(httpHandler != null){
                        //解析成功后即可进行处理了,socket将在函数内关闭
                        modelRouter(httpHandler, socket);
                        //Fix it key现在关闭是否会影响socketChannel？应该不不会吧，key代表一个事件的意思
                        key.cancel();

                    }else{
                        //Fix it. 解析错误说明http头部异常
                    }
                }
            }
        }

    }

    private void modelRouter(HTTPHandler httpHandler, Socket socket){

        //是否要考虑在执行此函数前将socket在selector中去掉？？？去不掉。。那是否考虑要key.cancle()??

        //文件相关
        if(httpHandler.getObject() == HTTPHandler.object.FILE){

            //***android端请求文件上传
            if(httpHandler.getMethod() == HTTPHandler.method.UPLOAD){
                //threadPoolSocketHandler.submit(new FileUploadThread(httpHandler, socket, fileUploadServerSocket));
            }


        }

        //字符消息相关
        if(httpHandler.getObject() == HTTPHandler.object.MESSAGE){

        }


        //走到这里的都是没有被解析的，所以这里需要进行50x的反馈，之后关闭socket

    }




}
