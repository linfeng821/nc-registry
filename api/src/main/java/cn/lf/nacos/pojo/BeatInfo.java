package cn.lf.nacos.pojo;


import lombok.Data;

import java.util.Map;


@Data
public class BeatInfo {

    private String namespaceId;
    private String ip;
    private int port;
    private String serviceName;
    private String clusterName;
    private Map<String,String> metadata;

    //private volatile long period;
}
