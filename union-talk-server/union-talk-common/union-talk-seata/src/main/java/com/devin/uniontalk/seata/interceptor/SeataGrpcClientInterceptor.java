package com.devin.uniontalk.seata.interceptor;

import com.devin.uniontalk.seata.constant.SeataGrpcConstant;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.seata.core.context.RootContext;
import org.springframework.util.StringUtils;

/**
 * 2026/05/20 19:30.
 *
 * <p>
 * Seata Grpc客户端拦截器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SeataGrpcClientInterceptor implements ClientInterceptor {

    /**
     * 拦截Grpc客户端调用并写入Seata全局事务XID.
     *
     * @param method      Grpc方法描述
     * @param callOptions 调用配置
     * @param next        下一个调用通道
     * @param <ReqT>      请求类型
     * @param <RespT>     响应类型
     * @return 客户端调用
     */
    //CHECKSTYLE:OFF
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions,
            final Channel next
    ) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            /**
             * 启动Grpc调用.
             *
             * @param responseListener 响应监听器
             * @param headers          请求头
             */
            @Override
            public void start(final Listener<RespT> responseListener, final Metadata headers) {
                String xid = RootContext.getXID();
                // 当前线程存在全局事务时，通过Grpc Metadata透传到服务端。
                if (StringUtils.hasText(xid)) {
                    headers.put(SeataGrpcConstant.XID_METADATA_KEY, xid);
                }
                super.start(responseListener, headers);
            }
        };
    }
    //CHECKSTYLE:ON
}
