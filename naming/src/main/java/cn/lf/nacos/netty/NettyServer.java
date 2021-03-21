package cn.lf.nacos.netty;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.config.NetConfig;
import cn.lf.nacos.core.ServiceManager;
import cn.lf.nacos.netty.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyServer {

    EventLoopGroup bossGroup;

    EventLoopGroup workerGroup;

    public static Channel channel;

    private ServerBootstrap bootstrap;

    private ChannelFuture channelFuture;

    @Autowired
    private NetConfig netConfig;

    @Autowired
    ServiceManager serviceManager;

    @Autowired
    AcceptorIdleStateTrigger idleStateTrigger;

    public void start() {
        try {
            //创建Event-LoopGroup
            //创建两个线程组bossGroup和workerGroup，含有的子线程NioEventLoop个数默认为cpu核数的二倍
            // bossGroup只是处理连接请求 ,真正的和客户端业务处理，会交给workerGroup完成
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            //创建服务器端的启动对象
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)//设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 初始化服务器连接队列大小，服务端处理客户端连接请求是顺序处理的,所以同一时间只能处理一个客户端连接。
                    // 多个客户端同时来的时候,服务端将不能处理的客户端连接请求放在队列中等待处理
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //对workerGroup的SocketChannel设置处理器
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new MessageEncoder());
                            pipeline.addLast(new IdleStateHandler(Constants.DEFAULT_RECEIVED_HEART_BEAT_INTERVAL, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(idleStateTrigger);
                            //IdleStateHandler的readerIdleTime参数指定超过5秒还没收到客户端的连接，
                            //会触发IdleStateEvent事件并且交给下一个handler处理，下一个handler必须
                            //实现userEventTriggered方法处理对应事件
                            //是针对每一个客户端的心跳连接，即你第一个客户端不断发心跳，另一个隔很久也不发心跳，还是会触发读空闲
                            pipeline.addLast(new MessageDecoder());
                            pipeline.addLast(new ServerHandler(serviceManager));
                        }
                    });
            //异步地绑定服务器，调用sync()方法阻塞等待直到绑定完成
            channelFuture = bootstrap.bind(netConfig.getNettyPort()).sync();
            channel = channelFuture.channel();
            log.info("netty server start");
        } catch (Exception e) {

        }

    }


    public void close() {
        try {
            //对通道关闭进行监听
            channelFuture.channel().close().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}