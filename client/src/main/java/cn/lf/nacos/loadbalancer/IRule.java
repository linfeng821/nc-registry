package cn.lf.nacos.loadbalancer;

import cn.lf.nacos.pojo.Instance;

import java.util.List;

public interface IRule {

    public Instance choose(List<Instance> instances);

}
