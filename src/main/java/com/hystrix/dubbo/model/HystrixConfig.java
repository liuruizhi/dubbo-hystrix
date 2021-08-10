package com.hystrix.dubbo.model;

import lombok.Data;
import lombok.ToString;

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
     */
    private int sleepWindowInMilliseconds = 5000;
    /**
     * 熔断触发错误率阈值, 超过50%错误触发熔断
     */
    private int errorThresholdPercentage = 50;
    /**
     * 熔断判断请求数阈值, 一个统计周期内（默认10秒）请求不少于requestVolumeThreshold才会进行熔断判断
     */
    private int requestVolumeThreshold = 50;
    /**
     * 这里可以禁用超时, 而采用dubbo的超时时间, 默认为true
     */
    private boolean executionTimeoutEnabled = true;
    /**
     * 当隔离策略为THREAD时，当执行线程执行超时时，是否进行中断处理，默认为true。
     */
    private boolean threadInterruptOnTimeout = true;
    /**
     * 执行超时时间，默认为1000毫秒，如果命令是线程隔离，threadInterruptOnTimeout=true，则执行线程将执行中断处理。
     */
    private int executionTimeoutInMilliseconds = 1000;
    /**
     * fallback方法的信号量配置，配置getFallback方法并发请求的信号量，如果请求超过了并发信号量限制，
     * 则不再尝试调用getFallback方法，而是快速失败，默认信号量为10
     */
    private int fallbackSemaphoreMaxConcurrentRequests = 500;
    /**
     * 隔离策略, 默认thread线程池隔离
     * THREAD,SEMAPHORE
     */
    private String executionIsolationStrategy = "SEMAPHORE";
    /**
     * 设置隔离策略为ExecutionIsolationStrategy.SEMAPHORE时，HystrixCommand.run()方法允许的最大请求数。
     * 如果达到最大并发数时，后续请求会被拒绝。
     */
    private int semaphoreMaxConcurrentRequests = 100;
}
