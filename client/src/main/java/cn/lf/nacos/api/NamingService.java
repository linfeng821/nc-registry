package cn.lf.nacos.api;

import cn.lf.nacos.pojo.Instance;

import java.util.List;

public interface NamingService {

    public void registryInstance(String ip,int port,String groupName,String serviceName,String nameSpaceId);

    public void serviceFound(String namespaceId);

    public List<Instance> selectInstances(String serviceName);

}
