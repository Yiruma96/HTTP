package http.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/*
* 这应当是一个能够处理协议的类，在发送端可以用于设置各种协议参数，在服务端可以用于对协议进行解析*/
/*HTTP是一个单向的协议，分成request与reponse两个部分，所以一定是有主动发起请求的一方，而另一方就是被动的接收者，相较于socket通信而言这其实是不合适的，因为socket中更着重于
* 双向的通信，两端既是发送者又是接收者，由此带来的困难是FTP控制端口的实现，所以如果想用http来实现文件传输的话，就只能有一个端口*/
/*运行机制大概应该是这样的：服务端是一直运行的，的任务就是接收数据后进行http解析，然后调用相应的处理模块，之后做一个返回，所以两端其实都需要运行着服务器负责接收消息，
* 同时也都有着处理对应问题的模块，这样其实双方既是C他lient，又是Server*/
/*应该是再需要实现一个客户端，客户端会对request的信息进行进一步的填充，比如关键的cookie与userAgent，另外需要负责信息的组织发送，负责对返回的信息进行头部与信息的查封，
所以客户端这个类是用来实现socket连接的*/
public class HTTPHandler {

    /*这个类的意义很关键，并不是标准HTTP里面的GET，POST等method，而是他们之后的url路径那一部分，
        在标准HTTP中，这部分其实起到了将让服务器将数据对应的转发到相应处理模块里的功能，
        所以这里我们既然没有那种路径的方式指定处理这些信息的模块，那就通过枚举类型来记录吧，所以这块是这门一个作用*/
    public enum method{
        /*用于文件上传*/
        UPLOAD,
        /*用于客户端请求登陆*/
        LOGIN,
        /*用于客户端请求注册*/
        REGISTER,
        /*用于客户请求文件上传进度*/
        PROCESS,
        /*用于客户端请求分析结果*/
        ANALYSIS,
        /*用于测试*/
        TEST
    }

    public enum object{
        /*消息传输*/
        MESSAGE,
        /*文件传输*/
        FILE,
    }

    public enum headType{
        /*为http请求头*/
        REQUEST,
        /*为http响应头*/
        REPONSE
    }

    public static List<String> paramBlackList = Arrays.asList("[","]","=","&");

    private static object object = null;
    private static method method = null;
    private String statusCode = null;
    private String httpVersion;

    /**
     * 里面可能含有的字段：
     * - Content-Range   响应头用于指明文件传输开始的位置以及结束的终点
     * - File-Name       用于指定文件的名字
     * - Open-Port       响应头用于指明文件传输开放的端口
     */
    private HashMap<String, String> headMap = new HashMap<String, String>();

    private HashMap<String, String> cookie = new HashMap<String, String>();

    private static int HTTP_HEAD_LENGTH_MAX = 1024;  //限制接收中的长度，如果超过此长度的话会返回错误
    private static int HTTP_CONTENT_LENGTH_MAX = 1024*1024*2;  //http消息体的长度限制，参照php的设置为2M
    private static int SOCKET_READ_TIME_OUT = 5000;  //socket读的超时时间设置为5s

    private ByteBuffer content = ByteBuffer.allocate(HTTP_CONTENT_LENGTH_MAX);  //http头部之后的内容

    /**
     * 用于服务器接收http请求时，首先serverSocket.accept() 拿到socket，之后将此socket的输入流传送到此函数中进行读取，函数将
     * 读取出来http头部以及消息体，注意里面可能存在各种异常
     */
    public static HTTPHandler readRequests(Socket socket) throws Exception {

        socket.setSoTimeout(SOCKET_READ_TIME_OUT);

        //读取头部
        HTTPHandler httpHandler = HTTPHandler.readHttpHead(socket, headType.REQUEST);

        //System.out.println(new String(httpHandler.requestGen()));

        //读取消息体
        String contentLengthStr = httpHandler.getFields("Content-Length");
        if(contentLengthStr == null){ // 没有Content-Length字段的话就不读取消息体了
            httpHandler.addContent(null);
        }else {
            int contentLength = Integer.valueOf(contentLengthStr); //可能存在转换异常
            httpHandler.addContent(HTTPHandler.readHttpContent(socket, contentLength));
        }

        return httpHandler;
    }

    /**
     * 服务器用于发送返回的http信息
     */
    public static void sendReponse(Socket socket, HTTPHandler response) throws Exception {

        socket.setSoTimeout(SOCKET_READ_TIME_OUT);
        OutputStream os = socket.getOutputStream();

        //发送消息头
        byte[] httpHead = response.responseGen(); //生成http头
        os.write(httpHead);

        //发送消息体
        byte[] httpContent = response.getContent();  //生成消息体
        if(httpContent != null){
            os.write(httpContent);
        }

    }

    /**
     * 用于发送一个request请求，不同的是这个http通信并不关心response的数据，也不会接收，因为socket发送很短的数据，所以可以认为是瞬间执行完的
     */
    public static void sendRequest(Socket socket, HTTPHandler request) throws Exception{

        OutputStream os = socket.getOutputStream();

        //发送消息头
        byte[] httpHead = request.responseGen(); //生成http头
        os.write(httpHead);

        //发送消息体
        byte[] httpContent = request.getContent();  //生成消息体
        if(httpContent != null){
            os.write(httpContent);
        }
    }

    /**
     * 客户端用户发送一个请求头，并接收到响应头
     */
    public static HTTPHandler sendRequestAndReadReponse(Socket socket, HTTPHandler httpHandler) throws Exception {

        //这里需要注意下超时的时间设置为两倍，是因为首先时进行写操作，这时候可能会有服务器的读超时花费了一个超时时间，所以到接下来读取响应头的时候，又可能存在一个超时时间
        socket.setSoTimeout(SOCKET_READ_TIME_OUT*2);
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        //发送消息头
        byte[] httpHead = httpHandler.requestGen(); //生成http头
        os.write(httpHead);

        //发送消息体
        byte[] httpContent = httpHandler.getContent();  //生成消息体
        if(httpContent != null){
            //System.out.println(new String(httpContent));
            os.write(httpContent);
        }

        //接收reponse
        HTTPHandler httpHandlerReponse = HTTPHandler.readHttpHead(socket, headType.REPONSE);
        //System.out.println(httpHandlerReponse.showReponseHead());
        String contentLengthStr = httpHandlerReponse.getFields("Content-Length");
        if(contentLengthStr == null){ // 没有Content-Length字段的话就不读取消息体了
            httpHandler.addContent(null);
        }else {
            int contentLength = Integer.valueOf(contentLengthStr); //可能存在转换异常
            httpHandlerReponse.addContent(HTTPHandler.readHttpContent(socket, contentLength));
        }

        return httpHandlerReponse;

    }

    //从提取出http头部的剩下的流中，提取出来剩下的信息
    /*PS： 关于为什么要将两个流分开来，因为对于消息体的接收，是受head中字段的控制的，比如HEAD不接收消息体，指定content长度等等*/
    //根据http的结束方法，这里选取两个实现，一个是socket的关闭，一个是Content-Length的值
    //Fix it 考虑接收不满一直等待和socket中途断开连接的异常情况
    public static byte[] readHttpContent(Socket socket, int contentLength) throws Exception {

        InputStream is = socket.getInputStream();

        //content-length的长度不能超过规定的content最大长度的2M
        if(contentLength > HTTP_CONTENT_LENGTH_MAX){
            throw new Exception("http content length out of limit");
        }

        ByteBuffer buffer = ByteBuffer.allocate(contentLength);
        byte[] byteArray = new byte[32];  //每次接收32个字节
        int read = 0;


        //每次循环接收32个字节，直到将buffer接收满
        while(true){

            //读取一次 如果对方关闭连接的话，看作另一个消息体结束标志
            try {
                read = is.read(byteArray);
            } catch (IOException e) {
                //e.printStackTrace();
                break;
            }
            if(read != -1){

                //检测读取后是否总字节超过最大长度限制
                if((buffer.position() + read) > contentLength){
                    throw new Exception("http content length out of content length defined in head");
                }

                buffer.put(byteArray, 0, read);

                //检测是否接收完毕，然后跳出
                if(buffer.position() == contentLength){
                    break;
                }

            }
        }

        //返回buffer中收集的byte[]
        buffer.flip();
        int end = buffer.limit();
        byte[] retu = new byte[end];
        System.arraycopy(buffer.array(), 0, retu, 0, end);
        return retu;
    }

    //Fix it 没有考虑socket中途关闭或者一直没有数据接收的异常
    //输入一个socket输入流，返回一个httpHead以及剩下的输出流,也是一个静态工厂方法
    public static HTTPHandler readHttpHead(Socket socket, headType _headType) throws Exception {

        InputStream is = socket.getInputStream();

        HTTPHandler httpHandler;
        int read = 0;
        ByteBuffer httpHeadBuffer = ByteBuffer.allocate(HTTP_HEAD_LENGTH_MAX); // http头部限制在1024字节
        Charset charset = Charset.defaultCharset();
        byte[] singleByte = new byte[1];
        String httpHead;

        //开始逐字节接收，长度超出buffer以及接收到头部结束的时候，循环结束
        while(true){ //做http头部行的循环,每次接收一个字节，

            //检测lineBuffer是否超出极限长度，在http格式异常时可能存在此问题
            if (httpHeadBuffer.position() == HTTP_HEAD_LENGTH_MAX) {
                //Fix it 单行循环中可能出现问题抛出http格式异常的信息throw (Exception, "fwef");
                throw new Exception("http head length out of range");
            }

            //首先从is流中读出一个字节并存储到lineBuffer中  read方法可能存在Connection reset异常，不做处理，向上抛出
            read = is.read(singleByte);
            if (read == -1) {
                continue;
            } else {
                //System.out.println(new HexBinaryAdapter().marshal(singleByte));
                //System.out.println(new String(singleByte));
                httpHeadBuffer.put(singleByte, 0, read);
            }

            //尝试进行http头的解析
            httpHeadBuffer.flip();
            httpHead = charset.decode(httpHeadBuffer).toString();
            httpHandler = httpHeadParser(httpHead, _headType);
            if(httpHandler != null){
                return httpHandler;
            }

            //调整好lineBuffer状态，准备接收下一个byte
            httpHeadBuffer.position(httpHeadBuffer.limit());
            httpHeadBuffer.limit(httpHeadBuffer.capacity());

        }//http头部接收结束

    }

    //生成HTTPHandler类的工厂方法
    //如果检测到还没有接收完的话，会返回null，如果格式正常的话，处理过程中的异常会抛出，也就是解析终止
    private static HTTPHandler httpHeadParser(String httpHead, headType _headType) throws Exception{

        //对http头的一些基本检测
        if(!httpHead.endsWith("\r\n\r\n")){
            return null;
        }

        HTTPHandler httpHandlerRetu = new HTTPHandler();

        //对http头部进行分割并遍历解决每一行
        String[] httpHeadArray = httpHead.split("\r\n");
        Integer headLineLength = httpHeadArray.length;
        Integer count = 0;
        String headLine;
        String cookieLine = null;
        //System.out.println(Arrays.asList(httpHeadArray));
        while(count < headLineLength){
            headLine = httpHeadArray[count];
            //System.out.println(headLine);

            //http的头部一行的处理
            if(count == 0){
                //请求头类型 FILE UPLOAD HTTP/1.0
                //响应头类型 HTTP/1.0 200
                String[] httpHeadFirst = headLine.split(" ");
                //System.out.println(Arrays.asList(httpHeadFirst));

                //处理request第一行
                if(_headType == headType.REQUEST){
                    if(httpHeadFirst.length == 3){
                        httpHandlerRetu.setObject(HTTPHandler.object.valueOf(httpHeadFirst[0]));  //Object对象
                        httpHandlerRetu.setMethod(HTTPHandler.method.valueOf(httpHeadFirst[1]));  //method对象
                        httpHandlerRetu.setHttpVersion(httpHeadFirst[2]);             //httpVersion值
                    }
                    else{
                        throw new Exception("http head format wrong");
                    }
                }
                //处理reponse第一行
                if(_headType == headType.REPONSE){
                    if(httpHeadFirst.length == 2){
                        httpHandlerRetu.setHttpVersion(httpHeadFirst[0]);             //httpVersion值
                        httpHandlerRetu.setStatusCode(httpHeadFirst[1]);
                    }
                    else{
                        throw new Exception("http head format wrong");
                    }
                }
            }
            //处理http头部第一行之后的字段值
            else{
                String[] field = headLine.split(": ");
                //System.out.println(headLine);
                if(field.length == 2){
                    //System.out.println(headLine);

                    //请求头不收录Cookie头，相应头不收录Set-Cookie头，而是先存储到一个字符串中，稍后进行cookie相关处理
                    if(_headType == headType.REQUEST && field[0].equals("Cookie")){
                        cookieLine = field[1];
                        break;
                    }
                    if(_headType == headType.REPONSE && field[0].equals("Set-Cookie")){
                        cookieLine = field[1];
                        break;
                    }
                    httpHandlerRetu.setFields(field[0], field[1]);
                }
                else{
                    ;//格式字段错误，不处理这一行，但是可以接着运行
                }
            }

            count += 1;
        }

        //关于Cookie的处理，将cookie相关的字段解析到变量cookie中
        if(cookieLine != null){

            for (String cookieFieldStr : cookieLine.split("; ")) {
                String[] cookieField = cookieFieldStr.split("=");
                if(cookieField.length == 2){
                    httpHandlerRetu.setCookie(cookieField[0], cookieField[1]);
                }
            }
        }


        return httpHandlerRetu;
    }

    public byte[] requestGen() throws Exception {

        //请求头一定要有object, method 两个字段，检测其是否存在
        if(object == null){
            throw new Exception("request object is null");
        }
        if(method == null){
            throw new Exception("request method is null");
        }

        StringBuilder headBuilder = new StringBuilder();

        //添加第一行
        headBuilder.append(object.toString());
        headBuilder.append(" ");
        headBuilder.append(method.toString());
        headBuilder.append(" ");
        headBuilder.append((httpVersion == null)? "HTTP/1.0" : httpVersion);
        headBuilder.append("\r\n");
        //添加详细的head头
        for (Map.Entry<String, String> head : headMap.entrySet()) {
            headBuilder.append(head.getKey());
            headBuilder.append(": ");
            headBuilder.append(head.getValue());
            headBuilder.append("\r\n");
        }
        //添加cookie头
        String cookieStr = cookieToStr();
        if(!cookieStr.equals("")){
            headBuilder.append("Cookie: ");
            headBuilder.append(cookieToStr());
            headBuilder.append("\r\n");
        }

        //添加末尾的两个
        headBuilder.append("\r\n");

        return headBuilder.toString().getBytes();
    }

    public String showRequestHead() throws Exception {
        String requestHead = new String(requestGen());
        return requestHead.substring(0, requestHead.length()-4);
    }

    public byte[] responseGen() throws Exception {

        //响应头一定要有object，statusCode两个字段
        if(object == null){
            throw new Exception("reponse object is null");
        }
        if(statusCode == null){
            throw new Exception("reponse statusCode is null");
        }

        StringBuilder headBuilder = new StringBuilder();

        //添加第一行
        headBuilder.append((httpVersion == null)? "HTTP/1.0" : httpVersion);
        headBuilder.append(" ");
        headBuilder.append(statusCode);
        headBuilder.append("\r\n");
        //添加详细的head头
        for (Map.Entry<String, String> head : headMap.entrySet()) {
            headBuilder.append(head.getKey());
            headBuilder.append(": ");
            headBuilder.append(head.getValue());
            headBuilder.append("\r\n");
        }
        //添加cookie头
        String cookieStr = cookieToStr();
        if(!cookieStr.equals("")){
            headBuilder.append("Set-Cookie: ");
            headBuilder.append(cookieToStr());
            headBuilder.append("\r\n");
        }


        //添加末尾的两个
        headBuilder.append("\r\n");

        return headBuilder.toString().getBytes();
    }

    public String showReponseHead() throws Exception {
        String reponseHead = new String(responseGen());
        return reponseHead.substring(0, reponseHead.length()-4);
    }

//    public String hashMapToParam(HashMap<String, String>){
//
//    }

    public void setMethod(HTTPHandler.method method) {
        this.method = method;
    }

    public HTTPHandler.object getObject() {
        return object;
    }

    public void setObject(HTTPHandler.object object) {
        this.object = object;
    }

    public HTTPHandler.method getMethod() {
        return method;
    }

    public HashMap<String, String> getHeadMap() {
        return headMap;
    }

    public void setHeadMap(HashMap<String, String> headMap) {
        this.headMap = headMap;
    }

    public void setFields(String key, String value){

        //进行CRLF注入检测
        if(!key.contains("\r\n") && !key.contains("\r\n")){
            headMap.put(key, value);
        }

    }

    public String getFields(String fieldName){
        return this.headMap.get(fieldName);  //可能有存在和null两种情况
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        //检测状态码是否为纯数字
        try{
            Integer.parseInt(statusCode);
        }catch(NumberFormatException e){
            return;
        }

        this.statusCode = statusCode;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {

        String pattern = "HTTP/\\d.\\d";
        if(Pattern.matches(pattern, httpVersion)){
            this.httpVersion = httpVersion;
        }
    }

    public byte[] getContent() {
        int end = this.content.position();
        byte[] retu = new byte[end];
        System.arraycopy(this.content.array(), 0, retu, 0, end);
        return retu;
    }

    //这个函数供构造http请求用，所以会改变content长度
    public void setContent(byte[] content) {
        if(content.length+this.content.position() <= HTTP_CONTENT_LENGTH_MAX) {
            this.content.put(content);
            if (content != null) {
                setFields("Content-Length", String.valueOf(this.content.position()));
            }
        }
    }

    private boolean checkParam(Map.Entry<String, String> param){

        for (String wrongChar : paramBlackList) {
            if(param.getKey().contains(wrongChar))
                return false;
            if(param.getValue().contains(wrongChar))
                return false;
        }

        return true;
    }


    public static HashMap<String, String> paraStrToHashMap(String paraStr){

        HashMap<String, String> retu = new HashMap<>();

        for (String para : paraStr.split("&")) {
            String[] paraArray = para.split("=");
            if(paraArray.length == 2){
                retu.put(paraArray[0], paraArray[1]);
            }
        }

        return retu;

    }

    public void addPOST(HashMap<String, String> POST){

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> param : POST.entrySet()){
            if(checkParam(param)){
                stringBuilder.append(param.getKey());
                stringBuilder.append("=");
                stringBuilder.append(param.getValue());
                stringBuilder.append("&");
            }
        }

        if(stringBuilder.length() != 0){
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            setContent(stringBuilder.toString().getBytes());
        }

    }


    //这个函数供接收content用，不用于构造HTTP请求，所以不会改变content的长度
    private void addContent(byte[] content){
        if(content != null) {
            if (content.length <= HTTP_CONTENT_LENGTH_MAX) {
                this.content.put(content);
            }
        }
    }

    public boolean hasContent(){
        return (content==null)? false : true;
    }

    public void setCookie(String key, String value){
        cookie.put(key, value);
    }

    public String getCookie(String key){
        String value = cookie.get(key);
        if(value != null){
            return value;
        }

        return null;
    }

    public HashMap<String, String> getCookie() {
        return cookie;
    }

    private String cookieToStr(){
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry entry : cookie.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("; ");
        }
        if(stringBuilder.length() >= 2){
            stringBuilder.delete(stringBuilder.length()-2, stringBuilder.length());
        }

        return stringBuilder.toString();
    }

    public static HashMap<String, String> strToCookie(String cookie){

        HashMap<String, String> retu = new HashMap<>();

        for (String para : cookie.split("; ")) {
            String[] paraArray = para.split("=");
            if(paraArray.length == 2){
                retu.put(paraArray[0], paraArray[1]);
            }
        }

        return retu;
    }

    public static String hashMapToString(HashMap<String, String> hashMap){
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : hashMap.entrySet()){
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("\r\n");
        }

        return stringBuilder.toString();
    }


}
