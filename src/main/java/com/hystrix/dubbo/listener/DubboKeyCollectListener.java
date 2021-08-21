package com.hystrix.dubbo.listener;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yuanhuzi on 2021/8/21.
 */
@Component
public class DubboKeyCollectListener implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private AtomicInteger index = new AtomicInteger(0);
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (index.get() > 0) {
            return;
        }
        String applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        Set<String> commandKeys = HystrixDynamicSource.getCommandKey();
        Map<String, Object> header = new HashMap<>(1);
        header.put("Content-Type", "application/json;charset=utf-8");
        Map<String, Object> params = new HashMap<>(2);
        params.put("application", applicationName);
        params.put("keys", JSONObject.toJSONString(commandKeys));

        index.incrementAndGet();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
