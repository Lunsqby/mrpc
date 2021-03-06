package com.kongzhong.mrpc.transport.http;

import com.kongzhong.mrpc.model.RpcContext;
import com.kongzhong.mrpc.model.RpcRequest;
import com.kongzhong.mrpc.model.RpcResponse;
import com.kongzhong.mrpc.model.ServiceBean;
import com.kongzhong.mrpc.transport.SimpleResponseCallback;
import com.kongzhong.mrpc.utils.JSONUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.kongzhong.mrpc.Const.HEADER_REQUEST_ID;

/**
 * Http响应回调处理
 */
public class HttpResponseCallback extends SimpleResponseCallback<FullHttpResponse> {

    public static final Logger log = LoggerFactory.getLogger(HttpResponseCallback.class);

    private FullHttpResponse httpResponse;

    public HttpResponseCallback(RpcRequest request, FullHttpResponse httpResponse, Map<String, ServiceBean> serviceBeanMap) {
        super(request, null, serviceBeanMap);
        this.httpResponse = httpResponse;
    }

    @Override
    public FullHttpResponse call() throws Exception {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(request.getRequestId());
        try {
            Object result = handle(request);
            rpcResponse.setResult(result);
            if (null != request.getReturnType()) {
                rpcResponse.setReturnType(request.getReturnType().getName());
            }
            rpcResponse.setSuccess(true);
        } catch (Throwable e) {
            e = buildErrorResponse(e, rpcResponse);
            log.error("Rpc method invoke error", e);
        } finally {
            RpcContext.remove();
            String body = JSONUtils.toJSONString(rpcResponse);
            ByteBuf bbuf = Unpooled.wrappedBuffer(body.getBytes(CharsetUtil.UTF_8));
            httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
            httpResponse.headers().set(HEADER_REQUEST_ID, request.getRequestId());
            httpResponse.content().clear().writeBytes(bbuf);
            return httpResponse;
        }
    }

}