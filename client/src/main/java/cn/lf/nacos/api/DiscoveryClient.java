package cn.lf.nacos.api;

import cn.lf.nacos.pojo.Instance;

import java.util.List;

public interface DiscoveryClient {

    public List<Instance> getInstances(String serviceId);

}
