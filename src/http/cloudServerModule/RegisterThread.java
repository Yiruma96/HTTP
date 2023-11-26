package http.cloudServerModule;

import http.core.HTTPHandler;
import http.libs.DBUnit;
import http.libs.HandleRetu;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterThread implements Runnable {

    private String content;
    private Socket socket;

    public RegisterThread(Socket socket, String content){
        this.content = content;
        this.socket = socket;
    }

    private boolean check_information(String username,String password){
        String regEx = "^[a-zA-z0-9]{6,20}$";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher_username = pattern.matcher(username);
        Matcher matcher_pass = pattern.matcher(password);
        // 字符串是否与正则表达式相匹配
        boolean rs1 = matcher_username.matches();
        boolean rs2 = matcher_pass.matches();
        return rs1 && rs2;
    }

    //函数返回false以及函数报错，均为执行失败的标志
    private HandleRetu register(String username, String password) throws Exception {

        if(username == null || password == null){
            return new HandleRetu(false, "param is empty");
        }

        //对参数格式做检查
        if(!check_information(username, password)){
            return new HandleRetu(false, "param format wrong");
        }

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        conn = DBUnit.getConnection();
        //检测同名用户
        preparedStatement = conn.prepareStatement("select username from users where username=?");
        System.out.println(preparedStatement.toString());
        preparedStatement.setString(1, username);
        ResultSet rs = null;
        try {
            rs = preparedStatement.executeQuery();
        } catch (SQLException e) {
            //sql错误不能算作服务器错误抛出去，这里应做处理
            DBUnit.release(conn, preparedStatement, rs);
            return new HandleRetu(false, "sql exec wrong");
        }
        if (rs.next()) {
            if(username.equals(rs.getString("username"))){
                DBUnit.release(conn, preparedStatement, rs);
                return new HandleRetu(false, "same username has existed");
            }

        }
        DBUnit.release(conn, preparedStatement, rs);


        //生成存储的hash密码
        MessageDigest md = MessageDigest.getInstance("md5");
        md.update((password+"saltadchecksalt").getBytes());
        password = new HexBinaryAdapter().marshal(md.digest());


        //插入用户
        conn = DBUnit.getConnection();
        preparedStatement = conn.prepareStatement("insert users(username, password, is_admin) values(?,?,?)");
        System.out.println(preparedStatement.toString());
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);
        preparedStatement.setBoolean(3, false);
        if (preparedStatement.executeUpdate() == 1) {
            DBUnit.release(conn, preparedStatement, null);
            return new HandleRetu(true, "register success");
        }
        DBUnit.release(conn, preparedStatement, null);

        return new HandleRetu(false, "something error");
    }

    @Override
    public void run() {

        HTTPHandler reponse = new HTTPHandler();
        HandleRetu handleRetu = null;

        //从content中解析出来username与password
        HashMap<String, String> POST = HTTPHandler.paraStrToHashMap(content);
        try {
            handleRetu = register(POST.get("username"), POST.get("password"));
            if(handleRetu.getFlag()) {
                reponse.setStatusCode("200");
            }else{
                reponse.setStatusCode("409");
            }
        }catch(Exception e){
            handleRetu = new HandleRetu(false, "server error");
            reponse.setStatusCode("500");
        }

        reponse.setContent(handleRetu.getDesc().getBytes());

        try {
            HTTPHandler.sendReponse(this.socket, reponse);
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
            //e.printStackTrace();
        }
    }
}
