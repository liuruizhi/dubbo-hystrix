package com.hystrix.dubbo.listener;

import com.alibaba.fastjson.JSON;
import com.hystrix.dubbo.constants.HystrixConstants;
import com.hystrix.dubbo.model.HystrixConfig;
import com.hystrix.dubbo.model.HystrixModel;
import com.netflix.config.ConfigurationManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by yuanhuzi on 2021/8/21.
 */
@Slf4j
@Data
@Component
public class HystrixDynamicSource {

    /**
     * 存储最新的配置
     */
    private static final Set<String> COMMAND_KEY = new HashSet<>(256);
    private static final String COMMAND_PREFIX = "hystrix.command.";
    private static final String THREAD_POOL_PREFIX = "hystrix.threadpool.";

    private static final ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(1);

    @PostConstruct
    public void schedule() {
        pool.scheduleAtFixedRate(() -> {
            log.info("refresh {}", System.currentTimeMillis());
            fillConfig();
        }, 30, 20, TimeUnit.SECONDS);
    }

    public static Set<String> getCommandKey() {
        return COMMAND_KEY;
    }

    public static void collectKey(String commandFullKey) {
        if (StringUtils.isBlank(commandFullKey)) {
            return;
        }
        COMMAND_KEY.add(commandFullKey);
    }

    public void fillConfig() {

        if (COMMAND_KEY.isEmpty()) {
            return;
        }
        // 根据key从redis里取值
        COMMAND_KEY.forEach(fullKey -> {
            HystrixModel hystrixModel = config(fullKey);
            if (null == hystrixModel || null == hystrixModel.getHystrixConfig()) {
                return;
            }
            if (Objects.equals(hystrixModel.getEnable(), HystrixConstants.UNENABLE)) {
                return;
            }
            if (HystrixConstants.THREAD.equals(hystrixModel.getHystrixConfig().getExecutionIsolationStrategy())) {
                updateCommon(hystrixModel.getHystrixConfig(), fullKey);
                updateThreadPool(hystrixModel.getHystrixConfig(), fullKey);
            }

            if (HystrixConstants.SEMAPHORE.equals(hystrixModel.getHystrixConfig().getExecutionIsolationStrategy())) {
                updateCommon(hystrixModel.getHystrixConfig(), fullKey);
            }
        });

    }

    /**
     * 更新一些通用的属性
     *
     * @param hystrixConfig
     * @param commandKey
     * @see HystrixConfig#processCommand(String, String)
     */
    private void updateCommon(HystrixConfig hystrixConfig, String commandKey) {
        int index = commandKey.lastIndexOf(".") + 1;
        Map<String, Object> commandConfig = hystrixConfig.processCommand(COMMAND_PREFIX, commandKey.substring(index));

        if (!MapUtils.isEmpty(commandConfig)) {
            for (Map.Entry<String, Object> config : commandConfig.entrySet()) {
                ConfigurationManager.getConfigInstance().addProperty(config.getKey(), config.getValue());
            }
        }
    }

    /**
     * 更新线程池相关属性
     * 更新线程池时需同时更新通用属性
     *
     * @param hystrixConfig
     * @param commandKey
     */
    private void updateThreadPool(HystrixConfig hystrixConfig, String commandKey) {
        Map<String, Object> threadPoolConfig = hystrixConfig.processThreadPool(THREAD_POOL_PREFIX, commandKey);
        if (!MapUtils.isEmpty(threadPoolConfig)) {
            for (Map.Entry<String, Object> config : threadPoolConfig.entrySet()) {
                ConfigurationManager.getConfigInstance().addProperty(config.getKey(), config.getValue());
            }
        }
    }

    /**
     * 获取配置信息
     *
     * @param key
     * @return
     */
    protected static HystrixModel config(String key) {
        // TODO 获取配置内容（json内容）
        String value = "";
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            HystrixModel hystrix = JSON.parseObject(value, HystrixModel.class);
            return hystrix;
        } catch (Exception e) {
            log.error("[HYSTRIX-SDK]hystrixConfig to object fail", e);
        }
        return null;
    }
}
