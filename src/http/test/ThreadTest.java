package http.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadTest implements Runnable {

    private ConcurrentHashMap<String, Boolean> tag;
    public int a = 1;

    public ThreadTest(ConcurrentHashMap<String, Boolean> tag){
        this.tag = tag;
    }

    @Override
    public void run() {

        while(true){
            System.out.println(tag.get("aaa"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
