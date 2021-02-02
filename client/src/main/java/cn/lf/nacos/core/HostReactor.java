package cn.lf.nacos.core;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.naming.NamingProxy;
import cn.lf.nacos.netty.NettyClient;
import cn.lf.nacos.pojo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.ServiceInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.rmi.server.ServerCloneException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class HostReactor {

    @Autowired
    NamingProxy namingProxy;

    //客户端实例缓存Map   Map<serviceName,ServiceInfo>
    private Map<String, ServiceInfo> serviceInfoMap=new ConcurrentHashMap<>();

    private ScheduledExecutorService executor;

    public void getServiceInfo(String namespaceId){

        //先尝试客户端缓存map中获取实例列表
        Map<String,ServiceInfo> services=serviceInfoMap;

        //第一次实例map为空
        if(services.size()==0){
            //向服务端发送请求
            updateServiceNow(namespaceId);
        }
        //定时拉取服务器上的实例注册表
        scheduleUpdate(namespaceId);

    }

    public void updateServiceNow(String namespaceId){
        namingProxy.queryList(namespaceId);
    }

    public ServiceInfo getServiceInfo0(String serviceName){
        return serviceInfoMap.get(serviceName);
    }

    //返回客户端的缓存map
    public Map<String,ServiceInfo> getAllInstances(){
        return serviceInfoMap;
    }

    public void scheduleUpdate(String namespaceId){
        executor=new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread=new Thread(r);
                thread.setDaemon(true);
                thread.setName("client.naming.updater");
                return thread;
            }
        });
        //每15s拉取一次服务端的注册数据
        executor.scheduleWithFixedDelay(new UpdateTask(namespaceId),0, Constants.SERVICE_FOUND_REFRESH_INTEEVAL, TimeUnit.SECONDS);
    }

    public class UpdateTask  implements Runnable{

        private String namespaceId;

        public UpdateTask(String namespaceId){
            this.namespaceId=namespaceId;
        }
        @Override
        public void run() {
            namingProxy.queryList(namespaceId);
        }
    }

    //收到服务端传来的实例信息，设置到客户端的缓存中
    public void putService(Map<String, ServiceInfo> services){
        serviceInfoMap=services;
        log.info("更新客户端缓存成功，缓存实例map"+serviceInfoMap);

        //更新完成后，还要更新一下nettyClient里的nettyServers，servers，nettyServer
        //services里面的每个serviceInfo都包含所有健康列表的信息，所以只用随意拿一个就行
        String clusters=null;
        for(String key:serviceInfoMap.keySet()){
            ServiceInfo serviceInfo=serviceInfoMap.get(key);
            clusters=serviceInfo.getClusters();
            break;
        }
        List<String> servers=new ArrayList<>();
        List<String> nettyServers=new ArrayList<>();
        Map<String,String> mappingMap=new HashMap<>();
        String []str=clusters.split("##");
        for(int i=0;i<str.length;i++){
            String serverIp=str[i].split(",")[0];
            String nettyServerIp=str[i].split(",")[1];
            servers.add(serverIp);
            nettyServers.add(nettyServerIp);
            mappingMap.put(serverIp,nettyServerIp);
        }
        //把最新健康的列表以及对应关系重新赋值
        NettyClient.servers=servers;
        NettyClient.nettyServers=nettyServers;
        NettyClient.mappingMap=mappingMap;
        log.info("nettyClient可用的服务端ip更新了"+NettyClient.mappingMap);
    }
}
