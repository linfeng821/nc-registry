package cn.lf.nacos.consistency;

import cn.lf.nacos.core.Instances;
import jdk.nashorn.internal.ir.ContinueNode;
import lombok.SneakyThrows;
import org.javatuples.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service("consistencyService")
public class ConsistencyServiceImpl implements ConsistencyService{

    @Autowired
    TaskDispatcher taskDispatcher;

    //Map<namespaceId+"##"+serviceName,Instances>
    private Map<String,Instances> dataMap= new ConcurrentHashMap<>(1024);
    //容器,装着观察者即Service
    private Map<String, CopyOnWriteArrayList<RecordListener>> listeners = new ConcurrentHashMap<>();

    public volatile Notifier notifier=new Notifier();

    @PostConstruct
    public void init(){
        ScheduledExecutorService executorService=
                new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t=new Thread();
                        t.setName("naming.distro.notifier");
                        t.setDaemon(true);
                        return t;
                    }}
                );
        //启动后台线程来异步更新注册内存表，初始化时启动该线程
        executorService.submit(notifier);
    }

    class Notifier implements  Runnable{
        private BlockingDeque<Pair> tasks=new LinkedBlockingDeque<>(1024*1024);

        public void addTask(String key,String messageId){
            tasks.add(Pair.with(key,messageId));
        }

        @SneakyThrows
        @Override
        public void run() {
           while(true){
               Pair pair=tasks.take();
               if(pair==null){
                   continue;
               }
               log.info("后台线程有数据拿出来了"+pair);
               String key=(String)pair.getValue0();
               if(!listeners.containsKey(key)){
                   continue;
               }
               for(RecordListener listener:listeners.get(key)){
                   listener.onChange(pair.getValue1()==null?key+"##null":key+"##"+(String)pair.getValue1(),dataMap.get(key));
                   continue;
               }
           }
        }
    }

    @Override
    public void put(String key, Instances instances, String messageId) {
        onPut(key,instances,messageId);

    }

    public void onPut(String key,Instances instances,String messageId){
        dataMap.put(key,instances);
        notifier.addTask(key,messageId);
    }

    /**
     * 暂时没用到该方法，因为添加和删除都使用onPut方法了
     *
     * @param key
     */
    @Override
    public void remove(String key) {

    }

    @Override
    public void listen(String key, RecordListener listener) {
        if(!listeners.containsKey(key)){
            listeners.put(key,new CopyOnWriteArrayList<>());
        }
        if(listeners.get(key).contains(listener)){
            return ;
        }
        //添加观察者，listener就是Service实例
        listeners.get(key).add(listener);
    }

    @Override
    public void setInstance(String key, Instances instances) {
        dataMap.put(key,instances);
    }

    /**
     * 返回实例列表
     *
     * @return
     */
    @Override
    public Map<String, Instances> getInstances() {
        return dataMap;
    }


    @Override
    public void notifyCluster(String key) {
        taskDispatcher.addTask(key);
    }
}
