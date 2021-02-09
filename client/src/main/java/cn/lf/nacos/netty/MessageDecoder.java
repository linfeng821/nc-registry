package cn.lf.nacos.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MessageDecoder  extends ByteToMessageDecoder {

    int length = 0;

    //将得到的二进制字节码 -> MessageProtocol 数据包(对象)
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {


        if(in.readableBytes() > 4){

            if(length == 0){
                length=in.readInt();
            }

            if(in.readableBytes()< length){
                log.info("当前可读数据不够,继续等待");
                return;
            }

            byte [] content= new byte[length];
            in.readBytes(content);

            MessageProtocol messageProtocol=new MessageProtocol();
            messageProtocol.setLength(length);
            messageProtocol.setContent(content);
            out.add(messageProtocol);
        }
        length=0;
    }
}
