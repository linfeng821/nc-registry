package cn.lf.nacos.consistency;

import cn.lf.nacos.core.Instances;
import cn.lf.nacos.pojo.Instance;

import java.util.Map;

public interface ConsistencyService {

    void put(String key, Instances instances,String messageId);

    void remove(String key);

    void listen(String key,RecordListener listener);

    public void setInstance(String key, Instances instances);

    Map<String,Instances> getInstances();

    void notifyCluster(String key);
}
