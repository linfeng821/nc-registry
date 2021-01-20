package cn.lf.nacos.netty;

import lombok.Data;

@Data
public class MessageProtocol {
    //定义一次发送包体长度
    private int len;

    //一次发送包体内容
    private byte[] content;
}
