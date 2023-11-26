package http.libs;

import adstatic.Start;
import soot.G;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

public class AnalysisThread{

    private String filePath = "uploadAPK/";
    private String analysisPath = "analysisRetu/";

    public AnalysisThread() throws SQLException {

    }

    public void analysisStart() throws Exception{

        // 连接数据库
        // 因为是死循环，没有考虑好要释放连接的合适位置，就算了，等着自动回收把，反正应该是个持续占用的连接
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        conn = DBUnit.getConnection();

        LinkedList<String> apkToAnalysis = new LinkedList<>();
        while(true){

            apkToAnalysis.clear();

            // 从数据库中获取还没有分析的apk
            preparedStatement = conn.prepareStatement("select * from apk where is_analysis_over=0");
            rs = preparedStatement.executeQuery();
            while(rs.next()){
                apkToAnalysis.add(rs.getString("apk_hash"));
            }
            System.out.println("取出的结果为"+apkToAnalysis.toString());

            // 开始循环分析
            for (String fileHash : apkToAnalysis) {
                analysisStart(fileHash, conn, preparedStatement, rs);
            }

            // 一轮循环结束后休息10s，防止过快的循环
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println(e);
                //e.printStackTrace();
            }

        }
    }

    private void analysisStart(String fileHash, Connection conn, PreparedStatement preparedStatement, ResultSet rs) throws Exception {

        System.out.println("开始分析啦");
        //开始进行分析
        System.out.println(filePath+fileHash+".apk");
        String analysisRetu = Start.startCheck(filePath+fileHash+".apk");
        System.out.println("运行结束啦，结果为"+analysisRetu);
        //保存分析结果
        PrintWriter pw = new PrintWriter(new FileOutputStream(analysisPath + fileHash));
        pw.write(analysisRetu);
        pw.close();
        //将apk表中的分析结果置为1
        try{
            preparedStatement = conn.prepareStatement("update apk set is_analysis_over=? where apk_hash=?");
            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, fileHash);
            System.out.println("下面开始在数据库中置为1，语句为" + preparedStatement.toString());
            preparedStatement.executeUpdate();
            System.out.println("置1工作完成");
        }catch(Exception e){
            System.out.println("连接出现问题");
            System.out.println(e);
        }

    }

}
