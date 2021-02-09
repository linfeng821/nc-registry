package cn.lf.nacos.netty.handler;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.core.ServiceManager;
import cn.lf.nacos.netty.AcceptorIdleStateTrigger;
import cn.lf.nacos.netty.MessageProtocol;
import cn.lf.nacos.pojo.BeatInfo;
import cn.lf.nacos.pojo.Instance;
import cn.lf.nacos.pojo.ServiceInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    @Autowired
    ServiceManager serviceManager;

    //是否能够接收注册信息的标志位
    public static volatile boolean isStartFlag=false;

    public ServerHandler(ServiceManager serviceManager){
        this.serviceManager=serviceManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        String msgStr=new String(msg.getContent());
        log.info("有消息来了。。。。。。。。。。。。。。");
        if(msgStr.startsWith(Constants.BEAT_ROUND)&&msgStr.endsWith(Constants.BEAT_ROUND)){
            log.info("=====服务端接收到心跳消息如下======");
            BeatInfo beatInfo=JSON.parseObject(getRealMsg(msgStr),BeatInfo.class);
            log.info(beatInfo.toString());
            if(!AcceptorIdleStateTrigger.dataMap.containsKey(ctx.channel().remoteAddress())){
                AcceptorIdleStateTrigger.dataMap.put(ctx.channel().remoteAddress(),beatInfo.getNamespaceId()+"##"+beatInfo.getServiceName()+"##"+beatInfo.getIp()+"##"+beatInfo.getPort());
            }
            AcceptorIdleStateTrigger.readIdleTimesMap.put(ctx.channel().remoteAddress(),0);
        }else if(msgStr.startsWith(Constants.REGISTER_SERVICE_ROUND)&&msgStr.endsWith(Constants.REGISTER_SERVICE_ROUND)){
            log.info("=====服务端接收到注册消息如下======");
            Instance instance= JSON.parseObject(getRealMsg(msgStr),Instance.class);
            log.info(instance.toString());
            while(true){
                if(isStartFlag){
                    break;
                }
                Thread.sleep(500);
            }
            serviceManager.registerInstance(instance);
        }else if(msgStr.startsWith(Constants.SERVICE_FOUND_ROUND)&&msgStr.endsWith(Constants.SERVICE_FOUND_ROUND)){
            log.info("=====有实例来请求返回注册表的信息=====");
            String namespaceId=getRealMsg(msgStr);
            Map<String, ServiceInfo> services=serviceManager.getServices(namespaceId);
            String message=Constants.SERVICE_FOUND_ROUND+ JSONObject.toJSONString(services)+Constants.SERVICE_FOUND_ROUND;
            MessageProtocol messageProtocol=new MessageProtocol();
            messageProtocol.setLen(message.getBytes().length);
            messageProtocol.setContent(message.getBytes());
            ctx.writeAndFlush(messageProtocol);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info(ctx.channel().remoteAddress()+":上线了");
        //初始化连接时将读空闲次数置为0
        AcceptorIdleStateTrigger.readIdleTimesMap.put(ctx.channel().remoteAddress(),0);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(ctx.channel().remoteAddress()+":断线了......");
        deleteIPS(ctx);
        super.channelInactive(ctx);
    }

    public void deleteIPS(ChannelHandlerContext ctx){
        if(AcceptorIdleStateTrigger.dataMap.get(ctx.channel().remoteAddress())!=null){
            String [] str=AcceptorIdleStateTrigger.dataMap.get(ctx.channel().remoteAddress()).split("##");
            String namespaceId=str[0];
            String serviceName=str[1];
            String ip=str[2];
            String port=str[3];
            Instance deleteInstance =new Instance();
            deleteInstance.setNamespaceId(namespaceId);
            deleteInstance.setServiceName(serviceName);
            deleteInstance.setIp(ip);
            deleteInstance.setPort(Integer.valueOf(port));
            AcceptorIdleStateTrigger.readIdleTimesMap.remove(ctx.channel().remoteAddress());
            AcceptorIdleStateTrigger.dataMap.remove(ctx.channel().remoteAddress());
            serviceManager.removeInstance(namespaceId,serviceName,deleteInstance);
        }
    }

    public String getRealMsg(String msg){
        return msg.substring(Constants.PROTOCOL_LEN,msg.length()-Constants.PROTOCOL_LEN);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
