package cn.lf.nacos.loadbalancer;

import cn.lf.nacos.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BaseLoadBalancer implements ILoadBalancer {

    @Autowired
    private IRule iRule;

    @Override
    public Instance chooseInstance(List<Instance> instances) {
        return iRule.choose(instances);
    }
}
