package cn.lf.nacos.loadbalancer;

import cn.lf.nacos.pojo.Instance;

import java.util.List;

public interface ILoadBalancer {

    public Instance chooseInstance(List<Instance> instances);
}
