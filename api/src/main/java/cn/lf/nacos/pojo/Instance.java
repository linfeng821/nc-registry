package cn.lf.nacos.pojo;

import lombok.Data;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Data
public class Instance {
    
    private String namespaceId;

    private String ip;

    private int port;

    private String serviceName;

    private String clusterName;

    private Map<String,String> metadata=new HashMap<>();

    public URI getUri(){
        String uri="http://"+getIp()+":"+getPort();
        return URI.create(uri);
    }

    @Override
    public int hashCode(){
        return getIp().hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null || obj.getClass()!=getClass()){
            return false;
        }

        if(obj == this){
            return true;
        }

        Instance other=(Instance) obj;

        //namespaceId，serviceName，ip，port相同就认为两个对象相等
        return getNamespaceId().equals(other.getNamespaceId()) &&
                getServiceName().equals(other.getServiceName()) &&
                getIp().equals(other.getIp()) &&
                ( getPort() == other.getPort() );

    }
}
