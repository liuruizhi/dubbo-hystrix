package com.hystrix.dubbo.utils;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hystrix.dubbo.constants.HystrixConstants;
import com.hystrix.dubbo.model.HystrixModel;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * GroovyCacheUtil
 *
 * @author liuruizhi
 * @Date 2021/8/10
 **/
@Slf4j
public class GroovyCacheUtil {

    private static LoadingCache<String, Optional<GroovyObject>> localMemoryCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .refreshAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Optional<GroovyObject>>() {
                @Override
                public Optional<GroovyObject> load(String key) throws Exception {
                    return fetchData(key);
                }
            });


    private static Optional<GroovyObject> fetchData(String key) {
        if (StringUtils.isBlank(key) || key.lastIndexOf(HystrixConstants.H_LINE) <= 0) {
            return Optional.empty();
        }
        String[] keys = key.split(HystrixConstants.H_LINE);
        // TODO 获取配置内容（json内容）
        String value = "";
        if (StringUtils.isEmpty(value)) {
            return Optional.empty();
        }
        try {
            HystrixModel hystrix = JSON.parseObject(value, HystrixModel.class);
            log.info("[HYSTRIX-MODEL] is {}, str {}", hystrix, value);
            if (null == hystrix) {
                return Optional.empty();
            }
            // 降级脚本处理逻辑
            if (HystrixConstants.DEGRADE.equals(keys[1]) && StringUtils.isNotBlank(hystrix.getDegradeScript())) {
                String md5Key = getMd5Key(hystrix.getDegradeScript());
                GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
                Class clazz = groovyClassLoader.parseClass(hystrix.getDegradeScript(), md5Key);
                GroovyObject object = (GroovyObject) clazz.newInstance();

                return Optional.ofNullable(object);
            }
            // 熔断脚本处理逻辑
            if (HystrixConstants.FALLBACK.equals(keys[1]) && StringUtils.isNotBlank(hystrix.getFallbackScript())) {
                String md5Key = getMd5Key(hystrix.getFallbackScript());
                GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
                Class clazz = groovyClassLoader.parseClass(hystrix.getFallbackScript(), md5Key);
                GroovyObject object = (GroovyObject) clazz.newInstance();

                return Optional.ofNullable(object);
            }
        } catch (Exception e) {
            log.error("hystrixConfig to object fail", e);
        }
        return Optional.empty();
    }

    public static <T> T getValue(String key) {

        try {
            Optional<GroovyObject> value = localMemoryCache.get(key);
            if (value.isPresent()) {
                return (T) value.get();
            }
            return null;
        } catch (Exception ex) {
            log.error("获取缓存异常, key:{} ", key, ex);
        }
        return null;
    }

    /**
     * @param script
     * @return
     */
    private static String getMd5Key(String script) {
        // TODO Md5Utils.MD5Encode(script);对脚本进行MD5处理作为缓存key
        String key = "";

        return key;
    }
}
