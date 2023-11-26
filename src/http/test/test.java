package http.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.security.ntlm.Server;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class test {

    private method method;

    public enum method{
        /*信息传输中使用*/
        GET,
        /*文件传输中使用*/
        POST,
        /*用于文件传输控制*/
        FTC
    }

    private String methodToString(){
        String retu = "";

        switch (method){
            case POST:
                retu = "POST";
            case GET:
                retu = "GET";
            case FTC:
                retu = "FTC";
            default:
                retu = "";
        }

        return retu;
    }


    public static void main(String[] args) throws Exception {

//        HashMap<String, String> header = new HashMap<String, String>();
//        header.put("11", "22");
//        System.out.println(header.get("33"));

//        System.out.println(Integer.parseInt("11"));

//        String[]  test = new String[]{"11","22"};
//
//        String blackList = ".\"和我";
//        for (String aaa : test) {
//            System.out.println(aaa);
//        }

//        File file = new File("E:/htp.txt");
//        System.out.println(file.length());

//        System.out.println(Integer.parseInt("12a"));

//        System.out.println(String.valueOf(0x457));

//        ServerSocket serverSocket = new ServerSocket(0);
//        int port = serverSocket.getLocalPort();
//        System.out.println(serverSocket.getLocalPort());
//        //serverSocket.close();
//
//        ServerSocket serverSocket1 = new ServerSocket(port);
//        System.out.println(serverSocket1.getLocalPort());
//        serverSocket1.close();

//        File file = new File("E:/htp.txt");
//        System.out.println(file.isFile());

//        ByteBuffer buffer = ByteBuffer.allocate(1024);
//        System.out.println(buffer.limit());
//        System.out.println(buffer.capacity());
//        System.out.println(buffer.position());
//        byte[] a = "aaa".getBytes();
//        buffer.put(a,0,2);
//        buffer.flip();
//        int end = buffer.limit();
//        byte[] retu = new byte[end];
//        System.arraycopy(buffer.array(), 0, retu, 0, end);
//        System.out.println(new String(retu));

//        new HTTPServerTest();
//        new HTTPClientTest();

//        String aa = "FILE TEST HTTP/1.0";
//        System.out.println(aa.split(" ").length);
//        System.out.println(Arrays.asList(aa.split(" ")));

//        System.out.println(String.valueOf(456));
//        System.out.println(Integer.valueOf("456"));
//        System.out.println(new HexBinaryAdapter().marshal("\r".getBytes()));

//        String pattern = "HTTP/\\d.\\d";
//        System.out.println(Pattern.matches(pattern, "HTTP/2.0"));

//        String a = "";
        //System.out.println(new HexBinaryAdapter().marshal(a.getBytes()));
//        System.out.println(new String("".getBytes()));
        //System.out.println();

//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("456789; ");
//        if(stringBuilder.length() != 0){
//            stringBuilder.delete(stringBuilder.length()-2, stringBuilder.length());
//        }
//        String a = stringBuilder.toString();
//        if(a.equals("")){
//            System.out.println("hehe");
//        }
//        System.out.println(stringBuilder.toString());

//        Connection connection = null;
//        Class.forName("com.mysql.jdbc.Driver");


//        MessageDigest md = MessageDigest.getInstance("md5");
//        Random ran = new Random();
//        for(int i =0; i<100 ;i++){
//            System.out.println(String.valueOf(ran.nextInt()));
//            //update(String.valueOf(ran.nextInt()).getBytes());
//            System.out.println(new HexBinaryAdapter().marshal(md.digest()));
//        }

        //md.update();


//        HashMap<String, Object> test = new HashMap<String, Object>();
//        test.put("key","value");
//        String a = "1|4|5|6";
//        test.put("key1", 44);
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        ObjectOutputStream oos = new ObjectOutputStream(bao);
//        oos.writeObject(test);
//        //System.out.println(new String(bao.toByteArray()));
//
//        byte[] aaa = bao.toByteArray();
//        System.out.println(new HexBinaryAdapter().marshal(aaa));
//
//        ObjectInputStream ois= new ObjectInputStream(new ByteArrayInputStream(aaa));
       // HashMap<String, String> bbb = (HashMap<String, String>) ois.readObject();
//        System.out.println(bbb.get("eee"));

//        ObjectMapper mapper = new ObjectMapper();
//        //new TypeReference<HashMap<String, String>>();
//        String json = mapper.writeValueAsString(test);
//        System.out.println(json);

//        Connection conn = DBUnit.getConnection();
//        PreparedStatement preparedStatement = conn.prepareStatement("update users set sessio_id=?,data=? where username=?");
//        preparedStatement.setString(1, "session_id");
//        preparedStatement.setString(2, "data");
//        preparedStatement.setString(3, "yiruma");
//        try {
//            int ddd = preparedStatement.executeUpdate();
//        }catch (Exception e){
//            System.out.println(e);
//        }
        //System.out.println(ddd);


//        DBUnit.release(conn, preparedStatement, null);

//        Date d = new Date();
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");//设置日期格式
//        String time1 = df.format(d);
//        String time2 = df.format(new Date(d.getTime() + (long)1*24*60*60*1000));
//        System.out.println(time1);
//        System.out.println(time2);
//        System.out.println(time2.compareTo(time1));
//        System.out.println(df.format(new Date()));

        //登陆，分配session值
        //String session = (String)Session.start(null, "yiruma");
        //HashMap<String, String> _SESSIOM = (HashMap<String, String>)Session.start("F04080E8978098DDE7A5C46BC5654263",null);
//        _SESSIOM.put("aaa","bbb");
//        _SESSIOM.put("isLogin","1");
//        Session.updateSession(_SESSIOM, "F04080E8978098DDE7A5C46BC5654263");
        //System.out.println(_SESSIOM.toString());
//        System.out.println(Integer.valueOf("456ty"));

        //System.out.println(session);

//        byte[] retu = new byte[0];
//        System.arraycopy("aaaaaaa".getBytes(), 0, retu, 0, 0);
//        System.out.println(new String(retu).length());

//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("aaa&");
//        System.out.println(stringBuilder.length());
//        stringBuilder.deleteCharAt(stringBuilder.length()-1);
//        System.out.println(stringBuilder.toString());
//        System.out.println(stringBuilder.length());

//        String paraStr = "username=123&password=456";
//        HashMap<String, String> retu = new HashMap<>();
//
//        for (String para : paraStr.split("&")) {
//            String[] paraArray = para.split("=");
//            if(paraArray.length == 2){
//                retu.put(paraArray[0], paraArray[1]);
//            }
//        }
//        System.out.println(retu);


//        List<String> aaa = Arrays.asList(paraStr.split("&"));
//        System.out.println(aaa.size());

//        Connection conn = DBUnit.getConnection();
//        PreparedStatement preparedStatement = conn.prepareStatement("insert users(usename, password, is_admin) values(?,?,?)");
//        preparedStatement.setString(1, "aaa");
//        preparedStatement.setString(2, "11111111111111111111111111111111");
//        preparedStatement.setBoolean(3, false);
//        System.out.println(preparedStatement.executeUpdate());


//        String cookie = "buvid2=101E99CB-0A05-47D6-B2F4-040DE415F6D312645infoc; finger=edc6ecda; sid=lwgv95ck; buvid3=101E99CB-0A05-47D6-B2F4-040DE415F6D312645infoc; fts=1519887446; UM_distinctid=161e059eefed7-0273d271c899c7-e323462-100200-161e059eeff116";
//        HashMap<String, String> retu = new HashMap<>();
//
//        for (String para : cookie.split("; ")) {
//            String[] paraArray = para.split("=");
//            if(paraArray.length == 2){
//                System.out.println(paraArray[0]);
//                System.out.println(paraArray[1]);
//                retu.put(paraArray[0], paraArray[1]);
//            }
//        }
//        System.out.println(retu);

//        MessageDigest md = MessageDigest.getInstance("sha-256");
//        md.update("saltadchecksalt".getBytes());
//        System.out.println(new HexBinaryAdapter().marshal(md.digest()));
//
//        DecimalFormat df = new DecimalFormat("0.000");
//        System.out.println(df.format(999 / (float)1000 * 100));
        //System.out.println(df.format(999/(float)1000).split("\\.")[1]);


//        NumberFormat numberFormat = NumberFormat.getInstance();
//        // 设置精确到小数点后2位
//        numberFormat.setMaximumFractionDigits(2);
//        System.out.println(String.format("%.3f", 9999 / (float) 10000));
//        String result = numberFormat.format((float) 998 / (float) 1000);
//        System.out.println("num1和num2的百分比为:" + result + "%");

//        int currentSize = 900;
//        System.out.println(((currentSize - 900) / (float)10000));
//        if(((currentSize - 900) / (float)10000) >= 0.01){
//            System.out.println("haha");
//        }

//        Date date = new Date();
//        while(true){
//            System.out.println(date.getTime());
//            Thread.sleep(1000);
//        }

//        StringBuilder stringBuilder = new StringBuilder();
//        System.out.println(stringBuilder.toString().equals(""));

//        HashMap<String, Integer> test = new HashMap<>();
//        test.put("aaa", 11);
//        Integer a = test.get("aa");
//        System.out.println(a);

//        String a = "aabb";
//        for (String b : a.split("&")) {
//            System.out.println(b+"cc");
//        }

        String fileHash = "F2F263CD7A3820AB139A2C8041BEAC80D35B20F54453AABED810A281411DA59B";
        File analysisRetuFile = new File("analysisRetu/" + fileHash);
        Long length = analysisRetuFile.length();
        System.out.println(length.intValue());
        byte[] fileContent = new byte[length.intValue()];
        try (FileInputStream fis = new FileInputStream(analysisRetuFile)){
            fis.read(fileContent);
        } catch (IOException e) {
            System.out.println(e);
            //e.printStackTrace();
        }

        System.out.println(new String(fileContent));



    }
}
