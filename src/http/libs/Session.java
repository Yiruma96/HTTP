package http.libs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import http.libs.DBUnit;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class Session {

    public static Object start(String sessionID, String username){
        //如果用户名为空，说明不是从登陆界面过来的，基于sessionID查询，如果没有查到的话，说明还没有登陆，这时候应该告知用户登陆，
        //如果查到的话，取出data并判断这个session是否过期，如果没有过期的话，就获得session变量到运行空间中去

        if(username == null){
            if(sessionID == null){
                //告知用户登陆,返回一个bool值为false
                return false;
            }else{
                //为正常访问过程，在数据库中查找相应session的data数据，在判断是否过期后生成变量，异常情况下返回一个空的hashMap
                return loadSession(sessionID);
            }
        }else{
            //为正常登陆过程，为用户分配新的Session值，并更新在数据库中，异常情况下返回null
             return insertSession(username);
        }

    }

    public static String genSessionID() {

        try {
            Random random = new Random();
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(String.valueOf(random.nextInt()).getBytes());
            return new HexBinaryAdapter().marshal(md.digest());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Session ID 生成异常");
            e.printStackTrace();
        }
        return null;
    }

    private static String insertSession(String username) {

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String sessionID = null;
        try {
            String newSessionID = genSessionID();

            HashMap<String, String> _SESSION = new HashMap<String, String>();
            _SESSION.put("isLogin", "1");
            Date d = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");//设置日期格式
            String time = df.format(new Date(d.getTime() + (long)1*24*60*60*1000));
            _SESSION.put("Expires", time);
            String dataJSON = new ObjectMapper().writeValueAsString(_SESSION);

            conn = DBUnit.getConnection();
            preparedStatement = conn.prepareStatement("update users set session_id=?,data=? where username=?");
            preparedStatement.setString(1, newSessionID);
            preparedStatement.setString(2, dataJSON);
            preparedStatement.setString(3, username);

            if(preparedStatement.executeUpdate() == 1){
                sessionID = newSessionID;
            }
            DBUnit.release(conn, preparedStatement, null);
        }catch (Exception e){
            //是否需要日志类用于记录？
            //System.out.println(e);
        }

        DBUnit.release(conn, preparedStatement, null);
        return sessionID;
    }

    private static HashMap<String, String> loadSession(String sessionID){

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            HashMap<String, String> _SESSION = null;

            conn = DBUnit.getConnection();
            preparedStatement = conn.prepareStatement("select data from users where session_id=?");
            preparedStatement.setString(1, sessionID);
            rs = preparedStatement.executeQuery();
            if(rs.next()){
                String data = rs.getString("data");

                _SESSION = new ObjectMapper().readValue(data, new TypeReference<HashMap<String, String>>(){});
                //检测_SESSION是否过期
                if(_SESSION.containsKey("Expires")){
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");//设置日期格式
                    if(_SESSION.get("Expires").compareTo(df.format(new Date())) == 1){
                        return _SESSION;
                    }
                }

            }
        } catch (Exception e) {
            //是否考虑日志记录？
            //System.out.println(e);
        }

        DBUnit.release(conn, preparedStatement, rs);
        return new HashMap<String, String>();

    }

    public static boolean updateSession(HashMap<String, String> _SESSION, String sessionID){

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            //首先将_SESSION进行序列化
            String dataJSON = new ObjectMapper().writeValueAsString(_SESSION);

            //将新的data放到数据库中
            conn = DBUnit.getConnection();
            preparedStatement = conn.prepareStatement("update users set data=? where session_id=?");
            preparedStatement.setString(1, dataJSON);
            preparedStatement.setString(2, sessionID);
            //System.out.println(preparedStatement.toString());
            if(preparedStatement.executeUpdate() == 1){
                //System.out.println("haha");
                return true;
            }
        }catch(Exception e){
            //是否考虑日志记录？
            System.out.println(e);
        }

        DBUnit.release(conn, preparedStatement, null);
        return false;

    }

}
