package cn.lf.nacos.cluster;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("serverListManager")
public class ServerListManager {

    private List<Server> healthyServers =new ArrayList<>();

    public List<Server> getHealthyServers(){
        return healthyServers;
    }
}
