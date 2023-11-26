package http.libs;

import adstatic.tools.MyTools;
import com.fasterxml.jackson.core.util.InternCache;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.util.Map;

/*
create table users(
    username varchar(64) not null primary key,
    password char(32) not null,
    is_admin bool not null,
    session_id char(32),
    data varchar(256)
);

create table apk(
    apk_hash char(64) primary key,
    apk_size char(10),
    is_analysis_over bool not null
);

create table upload_process(
    apk_hash char(64) primary key,
    start char(10)
);
 */


public class DBUnit {

    //一个数据库连接池
    private static BasicDataSource dbcp;

    private static String DRIVER = "com.mysql.jdbc.Driver";
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static int INITSIZE;
    private static int MAX_WAIT;
    private static int MAX_IDLE;
    private static int MIN_IDLE;


    //使用静态代码块，所以程序一开始就会运行而且只会运行一次
    static{

        //读取配置文件
        Map<String, String> DBConfig = MyTools.getConfig("DATABASE");
        try{
            USER = DBConfig.get("USER");
            PASSWORD = DBConfig.get("PASSWORD");
            URL = DBConfig.get("URL");
            INITSIZE = Integer.valueOf(DBConfig.get("INITSIZE"));
            MAX_WAIT = Integer.valueOf(DBConfig.get("MAX_WAIT"));
            MAX_IDLE = Integer.valueOf(DBConfig.get("MAX_IDLE"));
            MIN_IDLE = Integer.valueOf(DBConfig.get("MIN_IDLE"));
        }catch (Exception e){
            System.out.println("读取数据库配置出现问题");
        }

        //初始化连接池
        dbcp = new BasicDataSource();

        //dbcp连接池的配置

        dbcp.setDriverClassName(DRIVER);       //设置驱动
        dbcp.setUrl(URL);                      //设置url
        dbcp.setUsername(USER);
        dbcp.setPassword(PASSWORD);
        dbcp.setInitialSize(INITSIZE);         //设置初始化连接数量
        dbcp.setMaxWaitMillis(MAX_WAIT);       //设置最大等待时间
        dbcp.setMinIdle(MIN_IDLE);             //设置最小空闲数
        dbcp.setMaxIdle(MAX_IDLE);             //设置最大空闲数

    }

    //获取数据库连接
    public static Connection getConnection() throws SQLException {
        return dbcp.getConnection();
    }

    //关闭数据库连接
    public static void release(Connection conn, Statement stmt, ResultSet rs){

        if(rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                System.out.println("数据库连接ResultSet释放失败");
                e.printStackTrace();
            }
        }
        if(stmt != null){
            try {
                stmt.close();
            } catch (SQLException e) {
                System.out.println("数据库连接Statement释放失败");
                e.printStackTrace();
            }
        }
        if(conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("数据库连接Connection释放失败");
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        Connection conn = getConnection();
        release(conn,null, null);
    }

}
