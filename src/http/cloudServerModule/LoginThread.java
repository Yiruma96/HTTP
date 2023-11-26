package http.cloudServerModule;

import http.core.HTTPHandler;
import http.libs.DBUnit;
import http.libs.HandleRetu;
import http.libs.Session;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class LoginThread implements Runnable {

    private String content;
    private Socket socket;


    public LoginThread(Socket socket, String content){
        this.content = content;
        this.socket = socket;
    }

    //函数返回false以及函数报错，均为执行失败的标志
    private HandleRetu login(String username, String password) throws Exception {

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        //生成存储的hash密码
        MessageDigest md = MessageDigest.getInstance("md5");
        md.update((password+"saltadchecksalt").getBytes());
        password = new HexBinaryAdapter().marshal(md.digest());

        //查询用户与密码是否匹配
        conn = DBUnit.getConnection();
        preparedStatement = conn.prepareStatement("select username from users where username=? and password=?");
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);
        try {
            rs = preparedStatement.executeQuery();
        } catch (SQLException e) {
            //sql错误不能算作服务器错误抛出去，这里应做处理
            DBUnit.release(conn, preparedStatement, rs);
            return new HandleRetu(false, "sql exec wrong");
        }
        if(rs.next()){
            if(username.equals(rs.getString("username"))){
                DBUnit.release(conn, preparedStatement, rs);
                return new HandleRetu(true, "login success");
            }
        }

        DBUnit.release(conn, preparedStatement, rs);
        return new HandleRetu(false, "not exist this user or password wrong");

    }


    @Override
    public void run() {
        HTTPHandler reponse = new HTTPHandler();
        HandleRetu handleRetu = null;

        //从content中解析出来username与password
        HashMap<String, String> POST = HTTPHandler.paraStrToHashMap(content);
        try {
            handleRetu = login(POST.get("username"), POST.get("password"));
            if(handleRetu.getFlag()) {
                //登陆成功后应该返回新的session值，将此用户置为登陆状态
                Object object = Session.start(null, POST.get("username"));
                if(object instanceof String){//其实这里验证是否为String，就已经将null的情况排除了
                    reponse.setCookie("session", (String) object);
                    reponse.setStatusCode("200");
                }else{
                    reponse.setStatusCode("409");
                }
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
