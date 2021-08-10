package com.hystrix.dubbo.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSON;
import com.hystrix.dubbo.command.DubboHystrixCommand;
import com.hystrix.dubbo.constants.HystrixConstants;
import com.hystrix.dubbo.model.HystrixModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * DubboHystrixFilter
 *
 * @author liuruizhi
 * @Date 2021/8/10
 **/
@Slf4j
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER})
public class DubboHystrixFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        String method = invocation.getMethodName();
        String interfaceName = invoker.getInterface().getName();
        String key = interfaceName + "." + method;

        HystrixModel hystrixModel = config(invocation.getParameterTypes(), key);
        // 单接口开关判断
        if (null == hystrixModel || Objects.equals(HystrixConstants.UNENABLE, hystrixModel.getEnable())) {
            return invoker.invoke(invocation);
        } else {
            DubboHystrixCommand command = new DubboHystrixCommand(invoker, invocation);
            return command.execute();
        }

    }

    private static HystrixModel config(Class<?>[] clazz, String key) {

        StringBuilder finalKey = new StringBuilder(key);
            if (null != clazz && clazz.length > 0) {
                for (int i = 0; i < clazz.length; i++) {
                    if (null == clazz[i]) {
                        continue;
                    }
                    if (0 == i) {
                        finalKey = finalKey.append(HystrixConstants.HASH_TAG).append(clazz[i].getSimpleName());
                        continue;
                    }
                    finalKey.append(HystrixConstants.COMMA).append(clazz[i].getSimpleName());
                }
            }
            log.info("[HYSTRIX-SDK] CommondKey:[{}]", finalKey.toString());
            // TODO 获取配置内容（json内容）
            String value = "";
            if (StringUtils.isEmpty(value)) {
                return null;
            }
            try {
                HystrixModel hystrix = JSON.parseObject(value, HystrixModel.class);
                log.info("[HYSTRIX-SDK][MODEL] is {}, str {}", hystrix, value);
                return hystrix;
            } catch (Exception e) {
                log.error("hystrixConfig to object fail", e);
            }
            return null;
    }
}
