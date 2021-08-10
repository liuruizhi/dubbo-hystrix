package com.hystrix.dubbo;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.fastjson.JSON;
import com.hystrix.dubbo.command.DubboHystrixCommand;
import com.hystrix.dubbo.model.HystrixConfig;
import com.hystrix.dubbo.model.HystrixModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.Assert;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * HystrixDubboTest
 *
 * @author liuruizhi
 * @Date 2021/7/13
 **/
@RunWith(PowerMockRunner.class)
//@PrepareForTest({ConfigUtils.class})
public class HystrixDubboTest {

    @Mock
    private Invoker<?> invoker;
    @Mock
    private Invocation invocation;

    private DubboHystrixCommand hystrixCommand;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);
        PowerMockito.when(invoker.getInterface()).then(new Answer<Class>() {
            @Override
            public Class answer(InvocationOnMock invocation) throws Throwable {

                return Object.class;
            }
        });

        PowerMockito.when(invocation.getMethodName()).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {

                return "get";
            }
        });

        PowerMockito.when(invocation.getParameterTypes()).then(new Answer<Class<?>[]>() {
            @Override
            public Class<?>[] answer(InvocationOnMock invocation) throws Throwable {

                return new Class<?>[]{String.class, int.class};
            }
        });

    }

    @Test
    public void testSuccess() {
//        PowerMockito.mockStatic(ConfigUtils.class);
//        PowerMockito.when(ConfigUtils.get(anyString())).thenReturn(mockConfig());

        hystrixCommand = new DubboHystrixCommand(invoker, invocation);
        PowerMockito.when(invoker.invoke(invocation)).thenReturn(succResponse());
        Result result = hystrixCommand.execute();
        Assert.assertEquals(result.getValue(), "SUCC");
    }

    @Test
    public void testException() {
//        PowerMockito.mockStatic(ConfigUtils.class);
//        PowerMockito.when(ConfigUtils.get(anyString())).thenReturn(mockConfig());

        hystrixCommand = new DubboHystrixCommand(invoker, invocation);
        PowerMockito.when(invoker.invoke(invocation)).thenReturn(exceptionResponse());
        Result result = hystrixCommand.execute();

        Assert.assertEquals(result.getValue(), null);
    }

    @Test
    public void testFallback() throws InterruptedException {
//        PowerMockito.mockStatic(ConfigUtils.class);
//        PowerMockito.when(ConfigUtils.get(anyString())).thenReturn(mockConfig());

        PowerMockito.when(invoker.invoke(invocation)).thenReturn(exceptionResponse());
        TimeUnit.SECONDS.sleep(5);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 450; i++) {
            hystrixCommand = new DubboHystrixCommand(invoker, invocation);
            Result result = hystrixCommand.execute();

//            Assert.assertEquals(result.getValue(), null);
        }
        System.out.println("TIME===" + (System.currentTimeMillis() - start));
    }

    // TODO 有个超时异常需找一下
    @Test
    public void testFallbackConsumer() {
//        PowerMockito.mockStatic(ConfigUtils.class);
//        PowerMockito.when(ConfigUtils.get(anyString())).thenReturn(consumerDegrade());

        hystrixCommand = new DubboHystrixCommand(invoker, invocation);
        Result result = hystrixCommand.execute();

        Assert.assertEquals(result.getValue(), "DegradeScript");

    }

    private String mockConfig() {

        HystrixModel hystrixModel = new HystrixModel();
        hystrixModel.setDegrade("0");
        hystrixModel.setDegradeScript("package com.dubbo.groovy\n" +
                "class GroovyDemo {\n" +
                "    \n" +
                "    def fallback() {\n" +
                "        \n" +
                "        return \"Degrade\"\n" +
                "    }\n" +
                "}");
        hystrixModel.setFallbackScript("package com.dubbo.groovy\n" +
                "\n" +
                "\n" +
                "class GroovyDemo {\n" +
                "\n" +
                "    def fallback() {\n" +
                "        \n" +
                "        return \"Fallback\"\n" +
                "    }\n" +
                "}");

        // 默认配置
        HystrixConfig hystrixConfig = new HystrixConfig();

        hystrixModel.setHystrixConfig(hystrixConfig);

        return JSON.toJSONString(hystrixModel);
    }

    private String consumerDegrade() {

        HystrixModel hystrixModel = new HystrixModel();
        hystrixModel.setDegrade("1");
        hystrixModel.setDegradeScript("package com.dubbo.groovy\n" +
                "\n" +
                "import com.alibaba.dubbo.rpc.RpcResult\n" +
                "\n" +
                "class GroovyDemo {\n" +
                "\n" +
                "    def fallback() {\n" +
                "        return \"DegradeScript\"\n" +
                "    }\n" +
                "}");
        hystrixModel.setFallbackScript("");

        // 默认配置
        HystrixConfig hystrixConfig = new HystrixConfig();

        hystrixModel.setHystrixConfig(hystrixConfig);

        return JSON.toJSONString(hystrixModel);
    }

    private Result succResponse() {
        RpcResult result = new RpcResult();
        result.setValue("SUCC");

        return result;
    }

    private Result exceptionResponse() {
        RpcResult result = new RpcResult();
        result.setException(new RpcException(1));

        return result;
    }
}
