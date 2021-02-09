package cn.lf.nacos.core;

import cn.lf.nacos.boot.SpringContext;
import cn.lf.nacos.consistency.RecordListener;
import cn.lf.nacos.pojo.Instance;
import cn.lf.nacos.push.PushService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.*;
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
        log.info("有事件触发了，观察者的onChange方法被调用了， key="+key+", value=" +value);
        updateIPS(value.getInstanceList(),key);
    }

    public void updateIPS(List<Instance> instanceList,String key){
        String messageId=key.substring(key.lastIndexOf("##")+2);
        Map<String,List<Instance>> ipMap=new HashMap<>(clusterMap);
        for(String clusterName : clusterMap.keySet()){
            ipMap.put(clusterName,new ArrayList<>());
        }
        for(Instance instance: instanceList){
            String clusterName = instance.getNamespaceId()+"##"+instance.getServiceName();
            if(instance==null){
                continue;
            }
            List<Instance> clusterIPS=ipMap.get(clusterName);
            if(clusterIPS==null){
                clusterIPS=new LinkedList<>();
                ipMap.put(clusterName,clusterIPS);
            }
            clusterIPS.add(instance);
        }
        clusterMap=ipMap;
        log.info("service的clusterMap"+clusterMap);
        getPushService().serviceChanged(this,messageId);
    }

    public PushService getPushService(){
        return  SpringContext.getAppContext().getBean(PushService.class);
    }

    @Override
    public void onDelete(String key) {

    }
}
