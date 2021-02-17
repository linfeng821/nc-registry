package cn.lf.nacos.cluster;


import cn.lf.nacos.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component("distroMapper")
@Repository
@Slf4j
public class DistroMapper implements ServerChangeListener{

    private List<String> healthyList=new ArrayList<>();

    @Autowired
    ServerListManager serverListManager;

    @PostConstruct
    public void init(){
        //注册观察者
        serverListManager.listen(this);
    }

    @Override
    public void onChangeServerList(List<Server> servers) {

    }

    @Override
    public void onChangeHealthyServerList(List<Server> latestReachableMembers) {
        List<String> newHealthyList=new ArrayList<>();
        for(Server server:latestReachableMembers){
            newHealthyList.add(server.getIp()+ Constants.IP_PORT_SPLITER+server.getServePort());
        }
        healthyList=newHealthyList;
        log.info("健康的Server列表更改了"+ healthyList);
    }
}
