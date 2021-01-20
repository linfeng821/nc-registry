package cn.lf.nacos.push;

import cn.lf.nacos.core.Service;
import org.springframework.context.ApplicationEvent;
/**
 * 服务发生改变的事件对象
 */
public class ServiceChangeEvent extends ApplicationEvent {

    private Service service;

    private String messageId;

    public ServiceChangeEvent(Object source,Service service,String messageId){
        super(source);
        this.service=service;
        this.messageId=messageId;
    }

    public Service getService(){
        return service;
    }

    public String getMessageId()
    {
        return messageId;
    }
}
