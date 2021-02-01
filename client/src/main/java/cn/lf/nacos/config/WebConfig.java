package cn.lf.nacos.config;

import cn.lf.nacos.loadbalancer.IRule;
import cn.lf.nacos.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
    /**
     * 测试替换默认策略
     * @return
     */
    @Bean
    public IRule iRule(){
        return new RandomRule();
    }
}
