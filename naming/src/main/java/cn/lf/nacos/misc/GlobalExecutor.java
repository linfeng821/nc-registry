package cn.lf.nacos.misc;

import cn.lf.nacos.common.Constants;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class GlobalExecutor {

    private static ScheduledExecutorService taskDispatchExecutor=
        new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t=new Thread();
                t.setDaemon(true);
                t.setName("naming.distro.task.dispatcher");
                return t;
            }
        });

    public static void submitTaskDispatch(Runnable runnable){
        taskDispatchExecutor.submit(runnable);
    }

    private static ScheduledExecutorService dataSyncExxcutor=
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t=new Thread();
                    t.setDaemon(true);
                    t.setName("naming.distro.data.sycner");
                    return t;
                }
            });


    public static void submitDataSync(Runnable runnable,long delay){
        dataSyncExxcutor.schedule(runnable,delay, TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService executorService
            =new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t=new Thread();
            t.setName("naming.timer");
            t.setDaemon(true);
            return t;
        }
    });

    //3分钟执行一次，如果上一个任务没有执行完毕，则需要等上一个任务执行完毕后立即执行，周期性执行任务。
    public static void registerServerListUpdater(Runnable runnable){
        executorService.scheduleAtFixedRate(runnable,0, Constants.SERVER_LIST_REFRESH_INTERVAL,TimeUnit.MINUTES);
    }

    private static ScheduledExecutorService notifyServerListExecutor=
            new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t=new Thread();
                    t.setDaemon(true);
                    t.setName("naming.server.list.notifier");
                    return t;
                }
            });

    public static void notifyServerListChange(Runnable runnable){
        notifyServerListExecutor.submit(runnable);
    }

    private static ScheduledExecutorService SERVER_STATUS_EXECUTOR
            =new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
             @Override
             public Thread newThread(Runnable r) {
                 Thread t=new Thread();
                 t.setDaemon(true);
                 t.setName("naming.status.worker");
                 return t;
             }
    });

    public static void registryServerStatusReporter(Runnable runnable,int delay){
        SERVER_STATUS_EXECUTOR.schedule(runnable,delay,TimeUnit.SECONDS);
    }
}
