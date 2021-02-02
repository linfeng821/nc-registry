package cn.lf.nacos.naming;

import cn.lf.nacos.api.NamingService;
import cn.lf.nacos.config.DiscoverProperties;
import cn.lf.nacos.core.HostReactor;
import cn.lf.nacos.netty.NettyClient;
import cn.lf.nacos.pojo.Instance;
import cn.lf.nacos.pojo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.descriptor.web.InjectionTarget;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.stereotype.Component;
import sun.java2d.SurfaceDataProxy;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component("nameingService")
public class NamingServiceImpl implements NamingService, InitializingBean {

    @Autowired
    DiscoverProperties discoverProperties;

    @Autowired
    NamingProxy namingProxy;

    @Autowired
    HostReactor hostReactor;

    @Autowired
    NettyClient nettyClient;

    /**
     *注册实例
     */
    @Override
    public void registryInstance(String ip, int port, String groupName, String serviceName, String nameSpaceId) {
        Instance instance=new Instance();
        instance.setNamespaceId(nameSpaceId);
        instance.setClusterName(groupName);
        instance.setIp(ip);
        instance.setPort(port);
        instance.setServiceName(serviceName);
        namingProxy.registerService(serviceName, instance);
    }

    /**
     * 服务发现
     * @param namespaceId
     * @return
     */
    @Override
    public void serviceFound(String namespaceId) {
        hostReactor.getServiceInfo(namespaceId);
    }

    /**
     * 返回所有实例
     */
    @Override
    public List<Instance> selectInstances(String serviceName) {
        ServiceInfo serviceInfo=hostReactor.getServiceInfo0(serviceName);
        List<Instance> instances=serviceInfo.getInstances();
        log.info("请求拿到instances"+instances);
        return instances;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        CountDownLatch countDownLatch=new CountDownLatch(1);

        class Task implements Runnable {

            private CountDownLatch countDownLatch;

            public Task(CountDownLatch countDownLatch){
                this.countDownLatch=countDownLatch;
            }
            @Override
            public void run() {
                try{
                    //等待netty启动好了才注册实例
                    countDownLatch.await();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                //注册服务
                registryInstance(discoverProperties.getClientIp(),discoverProperties.getClientPort(),discoverProperties.getClusterName(),discoverProperties.getService(),discoverProperties.getNamespace());
                serviceFound(discoverProperties.getNamespace());
            }
        }

        new Thread(new Task(countDownLatch)).start();

        nettyClient.start(countDownLatch);
    }
}
