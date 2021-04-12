package cn.lf.nacos.consistency;

import cn.lf.nacos.cluster.Server;
import cn.lf.nacos.common.Constants;
import cn.lf.nacos.config.NetConfig;
import cn.lf.nacos.core.Instances;
import cn.lf.nacos.misc.GlobalExecutor;
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
    //上次同步的时间
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
        GlobalExecutor.submitTaskDispatch(taskScheduler);
    }

    public void addTask(String key){
        taskScheduler.addTask(key);
    }

    class TaskScheduler implements Runnable{

        private BlockingQueue<String> queue=new LinkedBlockingQueue<>(128*1024);

        public void addTask(String key){
            //这个key不是表示啥，只是为了表示有数据进来了
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
                    //没有实例新增或删除，不用同步，跳过
                    //dataSize>0证明还没达到批量同步的数据或者刚同步的时间差少于2s，所以没进行同步,所以>0时还是需要同步
                    if(StringUtils.isBlank(key)&&dataSize<=0){
                        continue;
                    }
                    //没有健康的server了，跳过
                    if(dataSyncer.getServers()==null||dataSyncer.getServers().isEmpty()){
                        continue;
                    }
                    //同时只能一个server同步数据
                    redissonLock.lock();
                    //这里拿到的实例数据是暂时来说最新的，假如有两个server，两个client，
                    // client1向server1注册，server1的注册表有client1，client2向server2注册，server2的注册表有client2
                    // 假设server1先拿到锁，server2收到server1发来的集群同步消息，注册表这时有client1，client2
                    //然后server2拿到锁，再同步实例信息，这时server1的注册表也有client和client2了
                    Map<String, Instances> dataMap=consistencyService.getInstances();
                    dataSize++;

                    String serverAddr=netConfig.getServerIp()+Constants.IP_PORT_SPLITER+netConfig.getServerPort();
                    //异步批量处理
                    //新增的实例数或者删除的实例数(即发生改变的实例数)达到100时才会进行同步或者距离上次同步超过2s才会同步
                    //这里也防止了不断死循环的集群同步，但这里也有问题，如果同时有多个实例来注册到这个server，上次刚同步完，还没到2s，这时再同步的请求就被阻断了，这样集群一致性就不能保证了
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
