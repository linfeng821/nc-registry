package cn.lf.nacos.core;

import cn.lf.nacos.cluster.Server;
import cn.lf.nacos.pojo.Instance;
import cn.lf.nacos.pojo.ServiceInfo;
import cn.lf.nacos.push.ServiceChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.management.InstanceAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServiceManager implements ApplicationListener<ServiceChangeEvent> {
    
    
    //Map<namespaceId,Map<serviceName,Service>>  内存注册表
    private Map<String, Map<String,Service>> serviceMap=new ConcurrentHashMap<>();

    /**
     * 注册实例
     * @param instance
     */
    public void registerInstance(Instance instance){
        String namespaceId=instance.getNamespaceId();
        String serviceName=instance.getServiceName();
        createServiceIfAbsent(namespaceId,serviceName);
        Service service = getService(namespaceId,serviceName);
        log.info("service"+service);
        if(service==null){
            throw new RuntimeException("");
        }
        addInstance(namespaceId,serviceName,instance);
    }

    public void createServiceIfAbsent(String namespaceId,String serviceName){
        Service service=getService(namespaceId,serviceName);
        if(service==null){
            service=new Service();
            service.setNamespaceId(namespaceId);
            service.setName(serviceName);
            log.info("第一次创建该服务"+service);
            putService(service);
            service.init();
        }
    }

    public Service getService(String namespaceId,String serviceName){
        if(serviceMap.get(namespaceId)==null){
            return null;
        }
        return serviceMap.get(namespaceId).get(serviceName);
    }

    public void putService(Service service){
        if(!serviceMap.containsKey(service.getNamespaceId())){
            serviceMap.put(service.getNamespaceId(),new ConcurrentHashMap<>(16));
        }
        serviceMap.get(service.getNamespaceId()).put(service.getName(),service);
    }

    public void addInstance(String namespaceId,String serviceName,Instance instance){
        Service service =getService(namespaceId,serviceName);
        List<Instance> instanceList=service.allIPs(namespaceId+"##"+serviceName);
        if(instanceList==null){
            instanceList=new ArrayList<>();
        }
        if(instanceList.contains(instance)){
            return;
        }
        instanceList.add(instance);
        Instances instances=new Instances();
        instances.setInstanceList(instanceList);

    }

    @Override
    public void onApplicationEvent(ServiceChangeEvent serviceChangeEvent) {

    }

    public Map<String, ServiceInfo> getServices(String namespaceId){
        Map<String,Service> serviceMap0= serviceMap.get(namespaceId);
        Map<String,ServiceInfo> serviceInfoMap=new ConcurrentHashMap<>();
        for(Map.Entry<String,Service> entry: serviceMap0.entrySet()){
            Service service=entry.getValue();
            ServiceInfo serviceInfo=new ServiceInfo();
            serviceInfo.setName(service.getName());
            serviceInfo.setInstances(service.getClusterMap().get(service.getNamespaceId()+"##"+service.getName()));

            serviceInfoMap.put(service.getName(),serviceInfo);
        }
        return serviceInfoMap;
     }

     public void removeInstance(String namespaceId,String serviceName,Instance instance){
        Service service=getService(namespaceId,serviceName);
        String key=namespaceId+"##"+serviceName;
        List<Instance> instanceList=service.allIPs(key);
        instanceList.remove(instance);
        Instances instances=new Instances();
        instances.setInstanceList(instanceList);

     }
}
