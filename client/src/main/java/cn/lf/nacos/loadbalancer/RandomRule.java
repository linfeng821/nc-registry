package cn.lf.nacos.loadbalancer;

import cn.lf.nacos.pojo.Instance;

import java.util.List;

public class RandomRule implements IRule {
    /**
     * 随机选择实例
     * @param instances
     * @return
     */
    @Override
    public Instance choose(List<Instance> instances) {
        int index = (int) (Math.random()*instances.size());
        return instances.get(index);
    }
}
