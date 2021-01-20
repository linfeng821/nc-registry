package cn.lf.nacos.consistency;


import cn.lf.nacos.core.Instances;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SyncTask {

    private Map<String, Instances>  dataMap =new HashMap<>();

    private String targetServer;

    private int retryCount;

    private long lastExecuteTime;
}
