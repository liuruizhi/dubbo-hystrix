package com.hystrix.dubbo.constants;

/**
 * HystrixConstants
 *
 * @author liuruizhi
 * @Date 2021/7/5
 **/
public class HystrixConstants {
    /**
     * 手动降级开关，1为打开，0为关闭
     */
    public static final String OPEN = "1";
    /**
     * 接口未启用SDK熔断降级
     */
    public static final String UNENABLE = "0";
    /**
     * 线程池
     */
    public static final String THREAD = "THREAD";
    /**
     * 信号量
     */
    public static final String SEMAPHORE = "SEMAPHORE";
    /**
     * 降级标识
     */
    public static final String DEGRADE = "0";
    /**
     * 熔断标识
     */
    public static final String FALLBACK = "1";
    /**
     * 分隔符
     */
    public static final String H_LINE = "-";
    /**
     * 井号
     */
    public static final String HASH_TAG = "#";
    /**
     * 逗号
     */
    public static final String COMMA = ",";
}
