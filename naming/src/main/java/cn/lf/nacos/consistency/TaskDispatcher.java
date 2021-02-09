package cn.lf.nacos.consistency;

import cn.lf.nacos.cluster.Server;
import cn.lf.nacos.common.Constants;
import cn.lf.nacos.config.NetConfig;
import cn.lf.nacos.core.Instances;
import cn.lf.nacos.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class TaskDispatcher {

    private Long lastDispatchTime = 0L;

    @Autowired
    ConsistencyService consistencyService;

    @Autowired
    DataSyncer dataSyncer;

    @Autowired
    NetConfig netConfig;

    private int dataSize=0;

    @Autowired
    Redisson redisson;

    public volatile TaskScheduler taskScheduler=new TaskScheduler();

    @PostConstruct
    public void init(){

    }
    public void addTask(String key){
        taskScheduler.addTask(key);
    }
    class TaskScheduler implements Runnable{

        private BlockingQueue<String> queue=new LinkedBlockingQueue<>(128*1024);

        public void addTask(String key){
            queue.offer(key);
        }

        @Override
        public void run() {
            while(true){
                String lockKey="lockKey";
                RLock redissonLock=redisson.getLock(lockKey);
                try{
                    //每隔2s就从阻塞队列中拿取数据，看有没有实例信息的变更，以便来同步到其他集群节点
                    String key=queue.poll(Constants.TASK_DISPATCH_PERIOD, TimeUnit.MILLISECONDS);

                    if(StringUtils.isBlank(key)&&dataSize<=0){
                        continue;
                    }

                    if(dataSyncer.getServers()==null||dataSyncer.getServers().isEmpty()){
                        continue;
                    }

                    redissonLock.lock();

                    Map<String, Instances> dataMap=consistencyService.getInstances();
                    dataSize++;

                    String serverAddr=netConfig.getServerIp()+Constants.IP_PORT_SPLITER+netConfig.getServerPort();

                    if(dataSize==Constants.BATCH_SYNC_KEY_COUNT||(System.currentTimeMillis()-lastDispatchTime)>Constants.TASK_DISPATCH_PERIOD){
                        for(Server member: dataSyncer.getServers()){
                            //跳过自己
                            if(serverAddr.equals(member.getKey())){
                                continue;
                            }
                            SyncTask syncTask=new SyncTask();
                            syncTask.setDataMap(dataMap);
                            syncTask.setTargetServer(member.getKey());
                            log.info("向"+syncTask.getTargetServer()+"开始同步数据"+syncTask.getDataMap());
                            dataSyncer.submit(syncTask,0);
                        }
                        dataSize=0;
                        lastDispatchTime=System.currentTimeMillis();
                    }

                }catch(Exception e){
                    log.error("同步数据失败"+e);
                }finally{
                    redissonLock.unlock();
                }
            }
        }
    }
}
