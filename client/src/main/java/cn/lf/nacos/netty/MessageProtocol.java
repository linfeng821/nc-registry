package cn.lf.nacos.netty;


import lombok.Data;

/**
 * 自定义协议包
 */
@Data
public class MessageProtocol {
    //定义一次发送包体内容
    private int length;
    //一次发送包体内容
    private byte[] content;
}
