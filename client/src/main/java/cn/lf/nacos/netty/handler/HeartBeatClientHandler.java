package cn.lf.nacos.netty.handler;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.core.HostReactor;
import cn.lf.nacos.netty.MessageProtocol;
import cn.lf.nacos.pojo.ServiceInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.ConstantCallSite;
import java.util.Date;
import java.util.Map;

@Slf4j
public class HeartBeatClientHandler extends SimpleChannelInboundHandler <MessageProtocol> {

    private HostReactor hostReactor;

    public HeartBeatClientHandler(HostReactor hostReactor){
        this.hostReactor=hostReactor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("激活时间是:"+new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("停止时间是:"+new Date());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol messageProtocol) throws Exception {
        String msgStr=new String(messageProtocol.getContent());
        if(msgStr.startsWith(Constants.DISCONNECT_ROUND)&&msgStr.endsWith(Constants.DISCONNECT_ROUND)){
            log.error("客户端收到服务端传来断开连接的消息"+getRealMsg(msgStr));
        }else if(msgStr.startsWith(Constants.SERVICE_FOUND_ROUND)&&msgStr.endsWith(Constants.SERVICE_FOUND_ROUND)){
            Map<String, ServiceInfo> services= JSON.parseObject(getRealMsg(msgStr),new TypeReference<Map<String,ServiceInfo>>(){});
            log.info("收到服务器传来的服务列表"+services);
            //收到服务器端的实例信息，设置到客户端的缓存中
            hostReactor.putService(services);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    //去除协议字符
    private String getRealMsg(String msg){
        return msg.substring(Constants.PROTOCOL_LEN,msg.length()-Constants.PROTOCOL_LEN);
    }
}
