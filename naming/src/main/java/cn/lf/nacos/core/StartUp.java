package cn.lf.nacos.core;

import cn.lf.nacos.netty.NettyServer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class StartUp implements InitializingBean {

    @Autowired
    NettyServer nettyServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        nettyServer.start();
    }
}
