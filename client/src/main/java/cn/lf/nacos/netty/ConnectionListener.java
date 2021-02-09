package cn.lf.nacos.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class  ConnectionListener  implements ChannelFutureListener {
    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
            boolean successd=channelFuture.isSuccess();
            if(!successd){
                log.error("重连失败...");
                channelFuture.channel().pipeline().fireChannelInactive();
            }else{
                log.info("重连服务器成功");
            }
    }
}
