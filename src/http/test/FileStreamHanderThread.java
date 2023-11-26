package http.test;

import java.io.*;
import java.net.Socket;
import java.util.RandomAccess;

public class FileStreamHanderThread implements Runnable{

    private boolean stop = false;
    private Socket socket;
    private int start;
    private int end;
    private int currentSize;
    private String downFilePath;

    private int bufferLength = 1024;

    public FileStreamHanderThread(Socket socket, String downFilePath ,int start, int end){
        this.socket = socket;
        this.start = start;
        this.end = end;
        this.currentSize = start;
        this.downFilePath = downFilePath;
    }

    @Override
    public void run() {
        int read = 0;
        byte[] buffer = new byte[bufferLength];

        try(DataInputStream dis = new DataInputStream(socket.getInputStream());
            RandomAccessFile oSavedFile = new RandomAccessFile(downFilePath, "rw");){

            oSavedFile.seek(start);  //根据传过来的协议进行随机文件读写的定位

            /* 持续循环接收文件，考虑这几种情况
               - 客户端正常发送完毕，并发送过来了发送完毕信号，不必通知线程，在循环内写入到预定长度时自动结束
               - 客户端中断发送，发送过来了中断信号，此时外部程序将线程stop位置为true，线程将在本轮循环后结束
            */
            while(!stop){
                if((read = dis.read(buffer)) != -1){

                    oSavedFile.write(buffer, currentSize, read);   //将buffer里的数据写入文件

                    currentSize += read;
                    if(currentSize == end) //如果接收到文件最后，则结束文件输入
                        break;
                }
                read = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean getStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public String getDownFilePath() {
        return downFilePath;
    }

    public int getBufferLength() {
        return bufferLength;
    }

    public void setDownFilePath(String downFilePath) {
        this.downFilePath = downFilePath;
    }

    public void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
    }

}
