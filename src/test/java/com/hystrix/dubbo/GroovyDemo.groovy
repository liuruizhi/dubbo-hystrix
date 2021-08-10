package com.hystrix.dubbo

import com.alibaba.dubbo.rpc.RpcResult

class GroovyDemo {

    def fallback() {
        return new RpcResult("DegradeScript")
    }
}