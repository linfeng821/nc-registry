package cn.lf.nacos.netty;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Component
public class NettyClient {

    public static Channel channel;

    //存储健康servers的ip列表
    public static List<String> servers=new ArrayList<>();

    //存储健康servers的netty的ip列表
    public static List<String> nettyServers=new ArrayList<>();

    public static Map<String,String> mappingMap=new HashMap<>();

    public static String nettyServer;

    public void init(){

    }

    public void start(CountDownLatch countDownLatch){

    }

    public void close(){

    }
}
