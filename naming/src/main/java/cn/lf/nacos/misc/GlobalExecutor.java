package cn.lf.nacos.misc;

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

}
