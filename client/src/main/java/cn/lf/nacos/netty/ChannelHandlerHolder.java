package cn.lf.nacos.netty;

import io.netty.channel.ChannelHandler;

public interface  ChannelHandlerHolder {

    ChannelHandler[] handlers();

}
