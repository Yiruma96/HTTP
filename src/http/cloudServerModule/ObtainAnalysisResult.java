package http.cloudServerModule;

import http.core.HTTPHandler;
import http.libs.DBUnit;
import http.libs.HandleRetu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ObtainAnalysisResult implements Runnable{

    private HTTPHandler requestHead;
    private Socket socket;
    private String analysisPath = "analysisRetu/";
    private String fileHash;

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet rs = null;

    public ObtainAnalysisResult(Socket socket, HTTPHandler requestHead){
        this.requestHead = requestHead;
        this.socket = socket;

        try {
            conn = DBUnit.getConnection();
        }catch(Exception e){
            System.out.println("数据库连接失败");
        }
    }

    private boolean isAnalysisOver(){
        String fileHash = requestHead.getFields("FILE-HASH");

        try{
            preparedStatement = conn.prepareStatement("select is_analysis_over from apk where apk_hash=?");
            preparedStatement.setString(1, fileHash);
            preparedStatement.executeQuery();
            rs = preparedStatement.executeQuery();
            if(rs.next()){
                if(rs.getBoolean("is_analysis_over")){
                    return true;
                }else{
                    return false;
                }
            }
        }catch(Exception e){
            System.out.println(e);
        }

        return false;
    }

    private boolean isUploaded(){
        try{
            preparedStatement = conn.prepareStatement("select * from apk where apk_hash=?");
            preparedStatement.setString(1, fileHash);
            rs = preparedStatement.executeQuery();
            if(rs.next()){
                return true;
            }
        }catch(Exception e){
            System.out.println(e);
        }

        return false;
    }


    private void reupload(){
        try{
            preparedStatement = conn.prepareStatement("delete from apk where apk_hash=?");
            preparedStatement.setString(1, fileHash);
            preparedStatement.executeUpdate();

            preparedStatement = conn.prepareStatement("delete from upload_process where apk_hash=?");
            preparedStatement.setString(1, fileHash);
            preparedStatement.executeUpdate();
        }catch(Exception e){
            System.out.println(e);
        }
    }

    @Override
    public void run() {

        HTTPHandler reponse = new HTTPHandler();
        reponse.setStatusCode("400");  //成功的话再置为200

        //检查数据库是否连通
        if(conn != null) {
            //检查是否存在FILE-HASH头
            if (requestHead.getFields("FILE-HASH") != null) {
                fileHash = requestHead.getFields("FILE-HASH");
                //检查apk是否上传过
                if (isUploaded()) {
                    //检查是否分析完成
                    if (isAnalysisOver()) {

                        File analysisRetuFile = new File(analysisPath + fileHash);
                        if (analysisRetuFile.exists()) {
                            //如果文件存在则读取文件内容并返回
                            Long length = analysisRetuFile.length();
                            byte[] fileContent = new byte[length.intValue()];
                            try (FileInputStream fis = new FileInputStream(analysisRetuFile)) {
                                fis.read(fileContent);
                            } catch (IOException e) {
                                System.out.println(e);
                                //e.printStackTrace();
                            }
                            reponse.setContent(fileContent);
                            reponse.setStatusCode("200");
                        } else {
                            //如果文件不存在，则将apk库中的对应项清空，也就是客户端需要重新上传此apk
                            reupload();
                            reponse.setContent("The Analysis Retu is missed. Please reupload your apk".getBytes());
                        }

                    } else {
                        reponse.setContent("Running".getBytes());
                    }
                } else {
                    reponse.setContent("APK has not uploaded".getBytes());
                }
            } else {
                reponse.setContent("Missing FILE-HASH Field".getBytes());
            }
        }else{
            reponse.setContent("database connection failed".getBytes());
        }

        try {
            HTTPHandler.sendReponse(this.socket, reponse);
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println(e);
        }

        DBUnit.release(conn, preparedStatement, rs);

    }
}
