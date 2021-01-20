package cn.lf.nacos.cluster;


import cn.lf.nacos.common.Constants;
import lombok.Data;

@Data
public class Server {

    private String ip;

    private int servePort;

    private String site;

    private int weight=1;

    private boolean alive=false;

    private long lastRefTime=0L;

    private String lastRefTimeStr;

    @Override
    public int hashCode(){
        int result= ip.hashCode();
        result=result*31+ servePort;
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this==obj){
            return true;
        }

        if(obj==null || getClass()!=obj.getClass()){
            return false;
        }

        Server server=(Server) obj;
        return server.getIp().equals(getIp()) &&
                server.getServePort() == getServePort();
    }

    public String getKey(){
        return ip+ Constants.IP_PORT_SPLITER + servePort;
    }
}
