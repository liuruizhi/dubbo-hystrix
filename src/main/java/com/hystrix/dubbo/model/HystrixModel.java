package com.hystrix.dubbo.model;

import com.hystrix.dubbo.constants.HystrixConstants;
import lombok.Data;
import lombok.ToString;

/**
 * HystrixModel
 *
 * @author liuruizhi
 * @Date 2021/6/29
 **/
@Data
@ToString
public class HystrixModel {

    /**
     * 接口熔断降级打开，默认不启用
     */
    private String enable = HystrixConstants.UNENABLE;
    /**
     * 手动降级开关，1为开，0为关
     */
    private String degrade;
    /**
     * 降级groovy脚本
     */
    private String degradeScript;
    /**
     * 熔断groovy脚本
     */
    private String fallbackScript;
    /**
     * 熔断相关配置
     */
    private HystrixConfig hystrixConfig = new HystrixConfig();
}
