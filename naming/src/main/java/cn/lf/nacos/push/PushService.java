package cn.lf.nacos.push;

import cn.lf.nacos.core.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PushService {

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    /**
     * 有service的实例改变了
     * @param service
     */
    public void serviceChanged(Service service,String messageId){
        applicationEventPublisher.publishEvent(new ServiceChangeEvent(this,service,messageId));
    }
}