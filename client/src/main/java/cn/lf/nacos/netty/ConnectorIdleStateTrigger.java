package cn.lf.nacos.netty;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.config.DiscoverProperties;
import cn.lf.nacos.pojo.BeatInfo;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//发送服务心跳
@Slf4j
@Component
@ChannelHandler.Sharable
public class ConnectorIdleStateTrigger extends SimpleChannelInboundHandler<MessageProtocol> {


    @Autowired
    DiscoverProperties discoverProperties;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState state = event.state();
            BeatInfo beatInfo = new BeatInfo();
            beatInfo.setNamespaceId(discoverProperties.getNamespace());
            beatInfo.setClusterName(discoverProperties.getClusterName());
            beatInfo.setIp(discoverProperties.getClientIp());
            beatInfo.setPort(discoverProperties.getClientPort());
            beatInfo.setServiceName(discoverProperties.getService());

            if(state == IdleState.WRITER_IDLE){
                MessageProtocol messageProtocol= new MessageProtocol();
                String message = Constants.BEAT_ROUND+ JSON.toJSONString(beatInfo) + Constants.BEAT_ROUND;
                messageProtocol.setContent(message.getBytes());
                messageProtocol.setLength(message.getBytes().length);
                ctx.writeAndFlush(messageProtocol);
                log.info("发送心跳");
            }
        }else{
            super.userEventTriggered(ctx,evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol messageProtocol) throws Exception {
        //传给下一个handler进行处理
        ctx.fireChannelRead(messageProtocol);
    }
}
