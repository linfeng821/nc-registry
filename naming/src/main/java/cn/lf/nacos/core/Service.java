package cn.lf.nacos.core;

import cn.lf.nacos.boot.SpringContext;
import cn.lf.nacos.consistency.RecordListener;
import cn.lf.nacos.pojo.Instance;
import cn.lf.nacos.push.PushService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Slf4j
public class Service implements RecordListener {

    private String namespaceId;

    private String Name;

    private String groupName;

    //Map<namespaceId##serviceName,List<Instance>>
    private Map<String, List<Instance>>  clusterMap= new ConcurrentHashMap<>();

    public List<Instance> allIPs(String serviceName){
        return clusterMap.get(serviceName);
    }

    public void init(){
        log.info("service实例的初始化方法被调用了");
    }

    @Override
    public void onChange(String key, Instances value) {

    }
    

    public PushService getPushService(){
        return  SpringContext.getAppContext().getBean(PushService.class);
    }

    @Override
    public void onDelete(String key) {

    }
}
