package com.kongzhong.mrpc.client.proxy;

import com.google.common.reflect.AbstractInvocationHandler;
import com.kongzhong.mrpc.client.cluster.HaStrategy;
import com.kongzhong.mrpc.client.cluster.LoadBalance;
import com.kongzhong.mrpc.client.cluster.loadblance.SimpleLoadBalance;
import com.kongzhong.mrpc.config.ClientConfig;
import com.kongzhong.mrpc.interceptor.*;
import com.kongzhong.mrpc.model.RpcRequest;
import com.kongzhong.mrpc.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;

import static com.kongzhong.mrpc.Const.INTERCEPTOR_NAME_PREFIX;

/**
 * 默认的客户端代理
 *
 * @author biezhi
 *         2017/4/28
 */
@Slf4j
public class SimpleClientProxy<T> extends AbstractInvocationHandler {

    // 负载均衡器
    protected LoadBalance loadBalance = new SimpleLoadBalance();

    // HA策略
    protected HaStrategy haStrategy = ClientConfig.me().getHaStrategy();

    protected boolean hasInterceptors;

    protected List<RpcClientInteceptor> interceptors;

    protected InterceptorChain interceptorChain = new InterceptorChain();

    public SimpleClientProxy(List<RpcClientInteceptor> interceptors) {
        this.interceptors = interceptors;
        if (null != interceptors && !interceptors.isEmpty()) {
            hasInterceptors = true;
            int pos = interceptors.size();
            log.info("Add interceptors {}", interceptors.toString());
            for (RpcClientInteceptor rpcInteceptor : interceptors) {
                interceptorChain.addLast(INTERCEPTOR_NAME_PREFIX + (pos--), rpcInteceptor);
            }
        }
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Exception {

        RpcRequest request = RpcRequest.builder()
                .appId(ClientConfig.me().getAppId())
                .requestId(StringUtils.getUUID())
                .methodName(method.getName())
                .className(method.getDeclaringClass().getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(args)
                .returnType(method.getReturnType())
                .build();

        if (!hasInterceptors) {
            return haStrategy.call(request, loadBalance);
        }

        Invocation invocation = new ClientInvocation(haStrategy, loadBalance, request, interceptors);
        Object result = invocation.next();
        return result;
    }

}
