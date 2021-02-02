package cn.lf.nacos.netty;

import cn.lf.nacos.config.DiscoverProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Data
@Slf4j
@Component
public class NettyClient {

    private Bootstrap bootstrap;

    private EventLoopGroup group;

    private ChannelFuture channelFuture;

    @Autowired
    DiscoverProperties discoverProperties;

    public static Channel channel;

    //存储健康servers的ip列表
    public static List<String> servers=new ArrayList<>();

    //存储健康servers的netty的ip列表
    public static List<String> nettyServers=new ArrayList<>();

    public static Map<String,String> mappingMap=new HashMap<>();

    public static String nettyServer;

    //初始化的index， 随机选择一个server进行通信
    public int index;

    //将discoverProperties中mappingMap包含的服务器server地址和netty地址
    //存储到nettyClient的servers，nettyServers中
    public void init(){
        mappingMap = discoverProperties.getMappingMap();
        if(mappingMap.size()>0){
            for(Map.Entry<String,String> entry: mappingMap.entrySet()){
                servers.add(entry.getKey());
                nettyServers.add(entry.getValue());
            }
        }else{
            servers.add(discoverProperties.getServerAddr());
            nettyServers.add(discoverProperties.getNettyServerAddr());
        }
        Random random= new Random(System.currentTimeMillis());
        index=random.nextInt(nettyServers.size());
        //随机选择一台server的netty的ip来连接，来进行下面的通信
        nettyServer=nettyServers.get(index);
    }

    public void start(CountDownLatch countDownLatch){
        try{
            //进行初始化
            init();

            //创建客户端启动对象
            bootstrap = new Bootstrap();

            //事件循环组
            group= new NioEventLoopGroup();
            bootstrap.group(group)
                    //使用NioSocketChannel做作为客户端的通道实现
                    .channel(NioSocketChannel.class);

            bootstrap.connect(nettyServer.split(":")[0],Integer.parseInt(nettyServer.split(":")[1])).sync();
            log.info("客户端连接上"+NettyClient.getKeyBuValue(nettyServer));
            channel = channelFuture.channel();
            log.info("连接服务器成功");
            countDownLatch.countDown();
        }catch(InterruptedException e){
            log.error("客户端连接失败");
            e.printStackTrace();
        }
    }

    public void close(){
        try{
            //对通道关闭进行监听
            channelFuture.channel().close().sync();
            group.shutdownGracefully();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    //根据 传入的nettyServerIp 通过mappingMap 找到对应的 serverIp
    public static String getKeyBuValue(String val){
        for(String key: mappingMap.keySet()){
            if(mappingMap.get(key)==val || mappingMap.get(key).equals(val)){
                return key;
            }
        }
        return null;
    }
}
