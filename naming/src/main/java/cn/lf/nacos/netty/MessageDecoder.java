package cn.lf.nacos.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MessageDecoder extends ByteToMessageDecoder {

    int length=0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        //将得到的二进制字节码-> MessageProtocol数据包（对象）
        if(in.readableBytes()>=4){
            //获取消息头所标识的消息体字节数组长度
           if(length==0){
               length=in.readInt();
           }
           //当当前可获取到的字节数小于实际长度，则直接返回，直到当前可获取的字节数等于实际长度
           if(in.readableBytes()<length){
                log.info("当前可读数据不够，继续等待");
                return ;
           }
           //读取完整的消息体字节数组
           byte [] content=new byte[length];
           in.readBytes(content);

           //封装成MessageProtocol对象，传递到下一个handler业务处理
           MessageProtocol messageProtocol=new MessageProtocol();
           messageProtocol.setContent(content);
           messageProtocol.setLen(length);
           out.add(messageProtocol);
        }
        length=0;
    }
}
