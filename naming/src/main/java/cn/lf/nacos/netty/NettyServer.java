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

    public void start(){
    try {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MessageEncoder());
                        pipeline.addLast(new IdleStateHandler(Constants.DEFAULT_RECEIVED_HEART_BEAT_INTERVAL,0,0, TimeUnit.SECONDS));
                        pipeline.addLast(idleStateTrigger);
                        pipeline.addLast(new MessageDecoder());
                        pipeline.addLast(new ServerHandler(serviceManager));
                    }
                });
        channelFuture = bootstrap.bind(netConfig.getNettyPort()).sync();
        channel=channelFuture.channel();
        log.info("netty server start");
    }catch(Exception e){

    }

    }


    public void close(){
        try{
            //对通道关闭进行监听
            channelFuture.channel().close().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
