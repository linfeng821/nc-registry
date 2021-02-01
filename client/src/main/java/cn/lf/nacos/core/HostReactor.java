package cn.lf.nacos.core;

import cn.lf.nacos.common.Constants;
import cn.lf.nacos.pojo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.ServiceInformation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class HostReactor {

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

    }

    public ServiceInfo getServiceInfo0(String serviceName){
        return serviceInfoMap.get(serviceName);
    }

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
        executor.scheduleWithFixedDelay(new UpdateTask(namespaceId),0, Constants.SERVICE_FOUND_REFRESH_INTEEVAL, TimeUnit.SECONDS);
    }

    public class UpdateTask  implements Runnable{

        private String namespaceId;

        public UpdateTask(String namespaceId){
            this.namespaceId=namespaceId;
        }
        @Override
        public void run() {

        }
    }
}
