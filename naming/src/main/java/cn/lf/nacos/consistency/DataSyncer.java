package cn.lf.nacos.consistency;

import cn.lf.nacos.cluster.Server;
import cn.lf.nacos.cluster.ServerListManager;
import cn.lf.nacos.misc.GlobalExecutor;
import cn.lf.nacos.misc.Message;
import cn.lf.nacos.misc.ServerSynchronizer;
import cn.lf.nacos.misc.Synchronizer;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSyncer {

    private Synchronizer synchronizer=new ServerSynchronizer();


    @Autowired
    private ServerListManager serverListManager;

    public List<Server> getServers(){
        return serverListManager.getHealthyServers();
    }

    public void submit(SyncTask syncTask,long delay){
        GlobalExecutor.submitDataSync(new Runnable() {
            @Override
            public void run() {
                byte [] data= JSON.toJSONBytes(syncTask.getDataMap());
                boolean success=synchronizer.syncData(syncTask.getTargetServer(),data);
                if(!success){
                    SyncTask syncTask1=new SyncTask();
                    syncTask1.setTargetServer(syncTask.getTargetServer());
                    syncTask1.setDataMap(syncTask.getDataMap());
                    retrySubmit(syncTask1,delay);
                }
            }
        },delay);
    }

    /**
     * 失败重发
     * @param syncTask
     */
    public void retrySubmit(SyncTask syncTask,long delay){
        Server server=new Server();
        server.setIp(syncTask.getTargetServer().split(":")[0]);
        server.setServePort(Integer.parseInt(syncTask.getTargetServer().split(":")[1]));
        if(!getServers().contains(server)){
            return ;
        }
        submit(syncTask,delay);
    }
}
