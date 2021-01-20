package cn.lf.nacos.netty;

import cn.lf.nacos.config.NetConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private void start(){
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
                        pipeline.addLast();
                        pipeline.addLast();
                        pipeline.addLast();
                        pipeline.addLast();
                        pipeline.addLast();
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
            channelFuture.channel().close().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
