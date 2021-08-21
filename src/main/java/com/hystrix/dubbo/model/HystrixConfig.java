package com.hystrix.dubbo.model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * HystrixConfig
 *
 * @author liuruizhi
 * @Date 2021/6/29
 **/
@Data
@ToString
public class HystrixConfig {
    /**
     * 熔断触发后多久恢复half-open状态，默认5s
     * circuitBreaker.sleepWindowInMilliseconds
     */
    private int sleepWindowInMilliseconds = 5000;
    /**
     * 熔断触发错误率阈值, 超过50%错误触发熔断
     * circuitBreaker.errorThresholdPercentage
     */
    private int errorThresholdPercentage = 50;
    /**
     * 熔断判断请求数阈值, 一个统计周期内（默认10秒）请求不少于requestVolumeThreshold才会进行熔断判断
     * circuitBreaker.requestVolumeThreshold
     */
    private int requestVolumeThreshold = 50;
    /**
     * 这里可以禁用超时, 而采用dubbo的超时时间, 默认为false
     * execution.timeout.enabled
     */
    private boolean executionTimeoutEnabled = false;
    /**
     * 当隔离策略为THREAD时，当执行线程执行超时时，是否进行中断处理，默认为true。
     * execution.isolation.thread.interruptOnTimeout
     */
    private boolean threadInterruptOnTimeout = true;
    /**
     * 执行超时时间，默认为1000毫秒，如果命令是线程隔离，threadInterruptOnTimeout=true，则执行线程将执行中断处理。
     * execution.isolation.thread.timeoutInMilliseconds
     */
    private int executionTimeoutInMilliseconds = 1000;
    /**
     * fallback方法的信号量配置，配置getFallback方法并发请求的信号量，如果请求超过了并发信号量限制，
     * 则不再尝试调用getFallback方法，而是快速失败，默认信号量为10
     * fallback.isolation.semaphore.maxConcurrentRequests
     */
    private int fallbackSemaphoreMaxConcurrentRequests = 500;
    /**
     * 隔离策略, 默认thread线程池隔离
     * THREAD,SEMAPHORE
     * execution.isolation.strategy
     */
    private String executionIsolationStrategy = "SEMAPHORE";
    /**
     * 设置隔离策略为ExecutionIsolationStrategy.SEMAPHORE时，HystrixCommand.run()方法允许的最大请求数。
     * 如果达到最大并发数时，后续请求会被拒绝。
     * execution.isolation.semaphore.maxConcurrentRequests
     */
    private int semaphoreMaxConcurrentRequests = 100;
    /**
     * corePoolSize默认10
     */
    private Integer coreSize = 2;
    /**
     * maximumPoolSize默认10
     */
    private Integer maximumSize = 5;
    /**
     * keepAliveTime默认1分钟
     */
    private Integer keepAliveTimeMinutes = 1;
    /**
     * maxQueueSize
     * 当设为－1，会使用SynchronousQueue，值为正时使用LinkedBlcokingQueue。
     * 该设置只会在初始化时有效，之后不能修改threadpool的queue size，除非reinitialising thread executor。默认－1
     */
    private Integer maxQueueSize = -1;
    /**
     * queueSizeRejectionThreshold默认5
     * 即使maxQueueSize没有达到，达到queueSizeRejectionThreshold该值后，请求也会被拒绝。
     * 因为maxQueueSize不能被动态修改，这个参数将允许我们动态设置该值。if maxQueueSize == -1，该字段将不起作用
     */
    private Integer queueSizeRejectionThreshold = 5;
    /**
     * allowMaximumSizeToDivergeFromCoreSize默认false
     * 如果corePoolSize和maxPoolSize设成一样（默认实现）该设置无效
     * 设置为true启用maxPoolSize
     */
    private Boolean allowMaximumSizeToDivergeFromCoreSize = true;
    /**
     * threadPoolRollingNumberStatisticalWindowInMilliseconds默认10_000
     */
    private Integer rollingStatisticalWindowInMilliseconds = 10_000;
    /**
     * threadPoolRollingNumberStatisticalWindowBuckets默认10
     */
    private Integer rollingStatisticalWindowBuckets = 10;

    public Map<String, Object> processCommand(String prefix, String commandKey) {
        Map<String, Object> result = new HashMap<>(64);
        String key = prefix + commandKey + ".";
        result.put(key + "circuitBreaker.sleepWindowInMilliseconds", getSleepWindowInMilliseconds());
        result.put(key + "circuitBreaker.errorThresholdPercentage", getErrorThresholdPercentage());
        result.put(key + "circuitBreaker.requestVolumeThreshold", getRequestVolumeThreshold());
        result.put(key + "execution.timeout.enabled", isExecutionTimeoutEnabled());
        result.put(key + "execution.isolation.thread.interruptOnTimeout", isThreadInterruptOnTimeout());
        result.put(key + "execution.isolation.thread.timeoutInMilliseconds", getExecutionTimeoutInMilliseconds());
        result.put(key + "fallback.isolation.semaphore.maxConcurrentRequests",
                getFallbackSemaphoreMaxConcurrentRequests());
        result.put(key + "execution.isolation.strategy", getExecutionIsolationStrategy());
        result.put(key + "execution.isolation.semaphore.maxConcurrentRequests", getSemaphoreMaxConcurrentRequests());
        return result;
    }

    public Map<String, Object> processThreadPool(String prefix, String commandKey) {
        Map<String, Object> result = new HashMap<>(16);
        String key = prefix + commandKey + ".";
        result.put(key + "coreSize", getCoreSize());
        result.put(key + "maximumSize", getMaximumSize());

        return result;
    }
}
