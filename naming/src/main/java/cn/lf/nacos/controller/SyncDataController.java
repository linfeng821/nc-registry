package cn.lf.nacos.controller;

import cn.lf.nacos.cluster.Server;
import cn.lf.nacos.consistency.ConsistencyService;
import cn.lf.nacos.consistency.ConsistencyServiceImpl;
import cn.lf.nacos.core.Instances;
import cn.lf.nacos.core.Service;
import cn.lf.nacos.core.ServiceManager;
import cn.lf.nacos.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class SyncDataController {

    @Autowired
    ServiceManager serviceManager;

    @Autowired
    ConsistencyServiceImpl consistencyService;

    @PutMapping("data/sync")
    public ResponseEntity<String> onSyncData(Map<String, Instances> dataMap){
        if(dataMap==null){
            log.error("集群同步，接受的实例为空！");
        }
        log.info("集群数据同步成功"+dataMap);
        for(Map.Entry<String, Instances> entry:dataMap.entrySet()){
            String namespaceId=entry.getKey().split("##")[0];
            String serviceName=entry.getKey().split("##")[1];
            if(serviceManager.getService(namespaceId,serviceName)==null){
                serviceManager.createServiceIfAbsent(namespaceId,serviceName);
            }
            Service service=serviceManager.getService(namespaceId,serviceName);
            List<Instance> otherInstanceList=entry.getValue().getInstanceList();
            List<Instance> instanceList=service.allIPs(namespaceId+"##"+serviceName);
            if(instanceList==null){
                instanceList=new ArrayList<>();
            }
            List<Instance> newInstanceList=null;
            //如果是实例新增的情况下
            if(otherInstanceList.size()>=instanceList.size()){
                newInstanceList= (List<Instance>)CollectionUtils.union(otherInstanceList,instanceList);
            }else if(otherInstanceList.size()<instanceList.size()){
                newInstanceList=(List<Instance>) CollectionUtils.subtract(instanceList,(List<Instance>)CollectionUtils.subtract(instanceList,otherInstanceList));
            }
            Instances instances=new Instances();
            instances.setInstanceList(newInstanceList);
            consistencyService.onPut(entry.getKey(),instances, UUID.randomUUID().toString());
        }
        return ResponseEntity.ok("ok");
    }
}
