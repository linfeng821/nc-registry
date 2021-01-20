package cn.lf.nacos.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<MessageProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageProtocol msg, ByteBuf out) throws Exception {
        //将字节数组的长度写进消息头，解决半包/粘包问题
        out.writeInt(msg.getLen());
        //写入内容的字节数组
        out.writeBytes(msg.getContent());
    }
}
