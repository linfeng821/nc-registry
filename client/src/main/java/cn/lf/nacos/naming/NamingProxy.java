package cn.lf.nacos.naming;


import cn.lf.nacos.common.Constants;
import cn.lf.nacos.netty.MessageProtocol;
import cn.lf.nacos.netty.NettyClient;
import cn.lf.nacos.pojo.Instance;
import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NamingProxy {

    /**
     * 发送注册信息
     * @param serviceName
     * @param instance
     */
    public void registerService(String serviceName, Instance instance){
        Channel channel= NettyClient.channel;
        MessageProtocol messageProtocol=new MessageProtocol();
        String message= Constants.REGISTER_SERVICE_ROUND+ JSON.toJSONString(instance)+Constants.REGISTER_SERVICE_ROUND;
        messageProtocol.setLength(message.getBytes().length);
        messageProtocol.setContent(message.getBytes());
        channel.writeAndFlush(message);
        log.info("实例:{} 注册服务:{}",instance,serviceName);
    }

    /**
     * 发送获取服务列表的请求
     * @param namespaceId
     * @return
     */
    public void queryList(String namespaceId){
        Channel channel=NettyClient.channel;
        MessageProtocol messageProtocol=new MessageProtocol();
        String message=Constants.SERVICE_FOUND_ROUND+namespaceId+Constants.SERVICE_FOUND_ROUND;
        messageProtocol.setLength(message.getBytes().length);
        messageProtocol.setContent(message.getBytes());
        channel.writeAndFlush(messageProtocol);
        log.info("客户端服务发现请求");
    }
}
