package cn.lf.nacos.cluster;


import cn.lf.nacos.common.Constants;
import cn.lf.nacos.config.NetConfig;
import cn.lf.nacos.consistency.ConsistencyService;
import cn.lf.nacos.misc.GlobalExecutor;
import cn.lf.nacos.misc.Message;
import cn.lf.nacos.misc.ServerSynchronizer;
import cn.lf.nacos.misc.Synchronizer;
import cn.lf.nacos.netty.NettyServer;
import cn.lf.nacos.netty.handler.ServerHandler;
import cn.lf.nacos.utils.SystemUtils;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.internal.util.privilegedactions.LoadClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("serverListManager")
public class ServerListManager {

    private Synchronizer synchronizer =new ServerSynchronizer();

    @Autowired
    private ConsistencyService consistencyService;

    @Autowired
    private NetConfig netConfig;

    //判断是否刚启动或者刚重启，第一次发送心跳
    private static boolean isFirstSendHeatBeat=true;

    //Map<serverIp:serverPort,timestamps>
    private Map<String,Long> distroBeats=new ConcurrentHashMap<>();

    //为了保存着最新的server更新心跳时间后的数据 Map<String, List<Server>>
    private Map<String,List<Server>> distroConfig=new ConcurrentHashMap<>();

    //心跳消息前缀
    private static final String LOCALHOST_SITE="cluster_status";

    //所有的server节点，包括健康的和非健康的
    private List<Server> servers=new ArrayList<>();

    private List<Server> healthyServers =new ArrayList<>();

    private List<ServerChangeListener> listeners=new ArrayList<>();

    public void listen(ServerChangeListener listener){
        listeners.add(listener);
    }

    public List<Server> getHealthyServers(){
        return healthyServers;
    }

    @PostConstruct
    public void init(){
        GlobalExecutor.registerServerListUpdater(new ServerListUpdater());
        GlobalExecutor.registryServerStatusReporter(new ServerStatusReporter(),1);
    }

    /**
     * 返回配置文件读取的server列表
     * @return
     */
    private List<Server> refreshServerList(){
        List<Server> result=new ArrayList<>();
        List<String> serverList=new ArrayList<>();
        try{
            serverList= SystemUtils.readClusterConf();
            log.info("server列表ip是 {}"+serverList);
        }catch(IOException e){
            log.error("读取集群配置文件失败"+e);
        }
        if(CollectionUtils.isNotEmpty(serverList)){
            for(int i=0;i<serverList.size();i++){
                String ip;
                int port;
                String server=serverList.get(i);
                ip=server.split(Constants.IP_PORT_SPLITER)[0];
                port=Integer.parseInt(server.split(Constants.IP_PORT_SPLITER)[1]);

                Server member=new Server();
                member.setIp(ip);
                member.setServePort(port);
                result.add(member);
            }
        }
        return result;
    }

    class ServerListUpdater implements  Runnable{

        @Override
        public void run() {
            try{
                List<Server> refreshedServers=refreshServerList();
                List<Server> oldServers= servers;

                boolean changed=false;

                List<Server> newServers =(List<Server>)CollectionUtils.subtract(refreshedServers,oldServers);
                if(CollectionUtils.isNotEmpty(newServers)){
                    servers.addAll(newServers);
                    changed=true;
                    log.info("server 列表更新了 new :{}  server:{} ",newServers.size(),newServers);
                }
                List<Server> deleteServers=(List<Server>)CollectionUtils.subtract(oldServers,refreshedServers);
                if(CollectionUtils.isNotEmpty(deleteServers)){
                    servers.removeAll(deleteServers);
                    changed=true;
                    log.info("server 列表更新了 dead:{} server:{}",deleteServers.size(),deleteServers);
                }
                if(changed){
                    notifyListeners();
                }
            }catch(Exception e){
                log.error("error while updating server list.",e);
            }
        }
    }

    private void notifyListeners(){
        GlobalExecutor.notifyServerListChange(new Runnable() {
            @Override
            public void run() {
                //通知观察者集群列表更改了
                for(ServerChangeListener listener:listeners){
                    listener.onChangeServerList(servers);
                    listener.onChangeHealthyServerList(healthyServers);
                }
            }
        });
    }

    class ServerStatusReporter implements Runnable{

        @Override
        public void run() {

            try{
                //自己当前的ip加端口
                String serverAddr=netConfig.getServerIp()+Constants.IP_PORT_SPLITER+netConfig.getServerPort();
                //心跳信息 cluster_status#192.168.74.1:9000#1612510879801#true#
                String status= LOCALHOST_SITE+"#"+serverAddr+"#"+System.currentTimeMillis()+"#"+isFirstSendHeatBeat+"#";

                checkHeartBeat();

                onReceiveServerStatus(status);

                if(!isFirstSendHeatBeat){
                    log.info("可以正常接收注册信息了...");
                    ServerHandler.isStartFlag=true;
                }

                List<Server> allServers=servers;

                if(allServers.size()>0){
                    for (Server s:allServers){
                        if(s.getKey().equals(serverAddr)){
                            continue;
                        }
                        Message message=new Message();
                        message.setData(status);
                        synchronizer.send(s.getKey(),message);
                    }
                }
                isFirstSendHeatBeat=false;
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                GlobalExecutor.registryServerStatusReporter(this,Constants.SERVER_STATUS_SYNCHRONIZATION_PERIOD_MILLIS);
            }
        }
    }

    private void checkHeartBeat(){

        log.info("检查server集群间的心跳");

        List<Server> allServers=distroConfig.get(LOCALHOST_SITE);

        if(CollectionUtils.isEmpty(allServers)){
            return ;
        }

        long now=System.currentTimeMillis();

        List<Server> newHealthyList= new ArrayList<>(allServers.size());

        for(Server s:allServers){
            Long lastBeat=distroBeats.get(s.getKey());
            if(lastBeat==null){
                continue;
            }
            s.setAlive(now-lastBeat<Constants.SERVER_EXPIRED_MILLS);
            if(s.isAlive()&&!newHealthyList.contains(s)){
                newHealthyList.add(s);
            }
        }
        if(!CollectionUtils.isEqualCollection(healthyServers,newHealthyList)){
            healthyServers=newHealthyList;
            notifyListeners();
        }

    }

    public void onReceiveServerStatus(String serverStatus){

        if(serverStatus.length()==0){
            return ;
        }
        //心跳信息 cluster_status#192.168.74.1:9000#1612510879801#true#
        String [] params=serverStatus.split("#");
        Server server=new Server();
        server.setSite(params[0]);
        server.setIp(params[1].split(Constants.IP_PORT_SPLITER)[0]);
        server.setServePort(Integer.parseInt(params[1].split(Constants.IP_PORT_SPLITER)[1]));
        server.setLastRefTime(Long.parseLong(params[2]));

        boolean isFirst=(params[3].equals("true"));

        Long lastBeat=distroBeats.get(server.getKey());

        long now = System.currentTimeMillis();

        if(lastBeat!=null){
            server.setAlive(now-lastBeat<Constants.SERVER_EXPIRED_MILLS);
        }

        distroBeats.put(server.getKey(),now);

        Date date = new Date(Long.parseLong(params[2]));
        server.setLastRefTimeStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));

        List<Server> list=distroConfig.get(server.getSite());

        if(list==null){
            list = new ArrayList<>();
            list.add(server);
            distroConfig.put(server.getSite(),list);
        }

        List<Server> tempServerList=new ArrayList<>();

        for(Server s: list){
            String serverId=s.getKey();
            if(serverId.equals(server.getKey())){
                tempServerList.add(server);
                continue;
            }
            tempServerList.add(s);
        }

        if(!tempServerList.contains(server)){
            tempServerList.add(server);
        }

        distroConfig.put(server.getSite(),tempServerList);

        if(isFirst){
            checkHeartBeat();
            String serverAddr=netConfig.getServerIp()+Constants.IP_PORT_SPLITER+netConfig.getServerPort();
            if(!server.getKey().equals(serverAddr)){
                consistencyService.notifyCluster(server.getKey());
            }
        }
    }
}
