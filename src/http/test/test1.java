package http.test;

import java.util.concurrent.ConcurrentHashMap;

public class test1 {

    public static void main(String[] args) throws Exception {

        ConcurrentHashMap<String, Boolean> tag = new ConcurrentHashMap<>();
        tag.put("aaa", false);
        new Thread(new ThreadTest(tag)).start();

        Thread.sleep(5000);
        tag.put("aaa", true);

    }
}
