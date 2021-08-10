package com.hystrix.dubbo.command;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.fastjson.JSON;
import com.hystrix.dubbo.constants.HystrixConstants;
import com.hystrix.dubbo.model.HystrixConfig;
import com.hystrix.dubbo.model.HystrixModel;
import com.hystrix.dubbo.utils.GroovyCacheUtil;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import groovy.lang.GroovyObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * DubboHystrixCommand
 *
 * @author liuruizhi
 * @Date 2021/6/24
 **/
@Slf4j
public class DubboHystrixCommand extends HystrixCommand<Result> {

    private Invoker<?> invoker;
    private Invocation invocation;
    /**
     * groovy脚本指定方法
     */
    private static final String FALLBACK = "fallback";

    public DubboHystrixCommand(Invoker<?> invoker, Invocation invocation) {
        // 构造HystrixCommand.Setter
        super(hystrixCommandSetter(invoker, invocation));
        this.invoker = invoker;
        this.invocation = invocation;

    }

    private static Setter hystrixCommandSetter(Invoker<?> invoker, Invocation invocation) {
        // interfaceName.methodName
        String key = getKey(invocation.getMethodName(), invoker.getInterface().getName(), invocation.getParameterTypes());

        HystrixModel hystrixModel = config(key);
        if (null == hystrixModel || null == hystrixModel.getHystrixConfig()) {
            hystrixModel = new HystrixModel();
        }
        HystrixConfig hystrixConfig = hystrixModel.getHystrixConfig();

        return Setter
                // 组名使用服务接口模块名称
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
                // 隔离粒度为接口方法, 但是同一个接口中的所有方法公用一个线程池, 各个服务接口的线程池是隔离的
                // 配置到这里, 就说明, 相同的接口服务, 相同的方法, 拥有相同的熔断配置策略
                .andCommandKey(HystrixCommandKey.Factory.asKey(
                        getCommandKey(invocation.getMethodName(), invocation.getParameterTypes())))
                // 熔断配置
                .andCommandPropertiesDefaults(hystrixCommandPropertiesSetter(hystrixConfig))
                // 线程池配置
                .andThreadPoolPropertiesDefaults(hystrixThreadPoolPropertiesSetter(hystrixConfig));
    }

    /**
     * 设置熔断参数
     *
     * @param hystrixConfig
     * @return
     */
    public static HystrixCommandProperties.Setter hystrixCommandPropertiesSetter(HystrixConfig hystrixConfig) {

        return HystrixCommandProperties.Setter()
                .withCircuitBreakerSleepWindowInMilliseconds(hystrixConfig.getSleepWindowInMilliseconds())
                .withCircuitBreakerErrorThresholdPercentage(hystrixConfig.getErrorThresholdPercentage())
                .withCircuitBreakerRequestVolumeThreshold(hystrixConfig.getRequestVolumeThreshold())
                .withExecutionTimeoutEnabled(hystrixConfig.isExecutionTimeoutEnabled())
                .withExecutionIsolationThreadInterruptOnTimeout(hystrixConfig.isThreadInterruptOnTimeout())
                .withExecutionTimeoutInMilliseconds(hystrixConfig.getExecutionTimeoutInMilliseconds())
                .withFallbackIsolationSemaphoreMaxConcurrentRequests(hystrixConfig.getFallbackSemaphoreMaxConcurrentRequests())
                .withExecutionIsolationStrategy(getIsolationStrategy(hystrixConfig))
                .withExecutionIsolationSemaphoreMaxConcurrentRequests(hystrixConfig.getSemaphoreMaxConcurrentRequests());

    }

    /**
     * 默认使用信号量模式
     */
    public static HystrixThreadPoolProperties.Setter hystrixThreadPoolPropertiesSetter(HystrixConfig hystrixConfig) {
        // 获取线程池配置
        return HystrixThreadPoolProperties
                .Setter()
                .withCoreSize(10)
                .withAllowMaximumSizeToDivergeFromCoreSize(true)
                .withMaximumSize(20)
                .withMaxQueueSize(-1)
                .withKeepAliveTimeMinutes(1);
    }

    /**
     * 获取隔离策略
     *
     * @param config
     * @return
     */
    public static HystrixCommandProperties.ExecutionIsolationStrategy getIsolationStrategy(HystrixConfig config) {
        String isolation = config.getExecutionIsolationStrategy();
        if (!isolation.equalsIgnoreCase(HystrixConstants.THREAD)
                && !isolation.equalsIgnoreCase(HystrixConstants.SEMAPHORE)) {
            isolation = HystrixConstants.SEMAPHORE;
        }
        if (isolation.equalsIgnoreCase(HystrixConstants.THREAD)) {
            return HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
        } else {
            return HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
        }
    }

    @Override
    protected Result run() throws Exception {
        String key = getKey();
        HystrixModel model = config(key);
        if (null == model) {
            log.info("[HYSTRIX-SDK]配置信息不存在");
            // 如果没有配置，继续走，但是如果调用抛出了异常达到阈值还是会触发熔断，只不过返回默认值
            return invoker.invoke(invocation);
        }
        if (Objects.equals(HystrixConstants.OPEN, model.getDegrade())) {
            return executeGroovy(model.getDegradeScript(), HystrixConstants.DEGRADE, key);
        } else {
            Result result = invoker.invoke(invocation);
            // 如果远程调用异常，抛出异常就会调用getFallback()方法去执行降级逻辑
            if (result.hasException() && result.getException() instanceof RpcException
                    && !((RpcException) result.getException()).isBiz()) {
                throw new HystrixRuntimeException(HystrixRuntimeException.FailureType.COMMAND_EXCEPTION,
                        DubboHystrixCommand.class, result.getException().getMessage(),
                        result.getException(), null);
            }

            return result;
        }
    }

    @Override
    protected Result getFallback() {
        log.error("come into fall back method,please check it!", getFailedExecutionException());
        log.error("the execution exception is here!", getExecutionException());

        String key = getKey();
        HystrixModel model = config(key);
        if (null == model) {
            return new RpcResult();
        }
        Result result = executeGroovy(model.getFallbackScript(), HystrixConstants.FALLBACK, key);
        // 为null说明触发了熔断
        return result;
    }

    /**
     * 获取配置信息
     *
     * @param key
     * @return
     */
    private static HystrixModel config(String key) {
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

    /**
     * 组装key
     * {interfaceName}.{method}#{参数数量}，如(interfaceName.method#0)
     *
     * @return
     */
    private String getKey() {
        String method = invocation.getMethodName();
        String interfaceName = invoker.getInterface().getName();
        StringBuilder key = new StringBuilder(interfaceName + "." + method);
        Class<?>[] clazz = invocation.getParameterTypes();
        if (null == clazz || clazz.length <= 0) {
            log.info("[HYSTRIX-SDK] KEY:[{}]", key.toString());
            return key.toString();
        }
        for (int i = 0; i < clazz.length; i++) {

            if (null == clazz[i]) {
                continue;
            }

            if (0 == i) {
                key = key.append(HystrixConstants.HASH_TAG).append(clazz[i].getSimpleName());
                continue;
            }

            key.append(HystrixConstants.COMMA).append(clazz[i].getSimpleName());
        }

        log.info("[HYSTRIX-SDK] KEY:[{}]", key.toString());
        return key.toString();
    }

    /**
     * 获取key
     *
     * @param method
     * @param interfaceName
     * @param clazz
     *             invocation参数类型
     * @return
     */
    private static String getKey(String method, String interfaceName, Class<?>[] clazz) {
        StringBuilder key = new StringBuilder(interfaceName + "." + method);
        if (null == clazz || clazz.length <= 0) {
            log.info("[HYSTRIX-SDK] Key:[{}]", key.toString());
            return key.toString();
        }
        for (int i = 0; i < clazz.length; i++) {

            if (null == clazz[i]) {
                continue;
            }

            if (0 == i) {
                key = key.append(HystrixConstants.HASH_TAG).append(clazz[i].getSimpleName());
                continue;
            }
            key.append(HystrixConstants.COMMA).append(clazz[i].getSimpleName());
        }

        log.info("[HYSTRIX-SDK] Key:[{}]", key.toString());
        return key.toString();
    }

    private static String getCommandKey(String method, Class<?>[] clazz) {
        StringBuilder key = new StringBuilder(method);
        if (null == clazz || clazz.length <= 0) {
            log.info("[HYSTRIX-SDK]CommondKey:[{}]", key.toString());
            return key.toString();
        }
        for (int i = 0; i < clazz.length; i++) {

            if (null == clazz[i]) {
                continue;
            }

            if (0 == i) {
                key = key.append(HystrixConstants.HASH_TAG).append(clazz[i].getSimpleName());
                continue;
            }
            key.append(HystrixConstants.COMMA).append(clazz[i].getSimpleName());
        }

        log.info("[HYSTRIX-SDK]CommondKey:[{}]", key.toString());
        return key.toString();
    }

    private Result executeGroovy(String script, String type, String key) {

        if (StringUtils.isEmpty(script)) {
            log.info("[HYSTRIX-SDK]groovy脚本为空.");
            return new RpcResult();
        }

        Object[] args = invocation.getArguments();
        GroovyObject cacheInstance = GroovyCacheUtil.getValue(key + HystrixConstants.H_LINE + type);

        if (null == cacheInstance) {
            return new RpcResult();
        }
        try {
            Object result = cacheInstance.invokeMethod(FALLBACK, args);
            return new RpcResult(result);

        } catch (Exception e) {
            log.error("[HYSTRIX-SDK]Groovy newInstance Exception", e);
        }

        return new RpcResult();
    }

}
