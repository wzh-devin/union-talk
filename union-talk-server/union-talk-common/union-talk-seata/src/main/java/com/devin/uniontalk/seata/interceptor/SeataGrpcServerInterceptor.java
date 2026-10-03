package com.devin.uniontalk.seata.interceptor;

import com.devin.uniontalk.seata.constant.SeataGrpcConstant;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.seata.core.context.RootContext;
import org.springframework.util.StringUtils;

/**
 * 2026/05/20 19:30.
 *
 * <p>
 * Seata Grpc服务端拦截器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SeataGrpcServerInterceptor implements ServerInterceptor {

    /**
     * 拦截Grpc服务端调用并绑定Seata全局事务XID.
     *
     * @param call    服务端调用
     * @param headers 请求头
     * @param next    下一个处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 服务端调用监听器
     */
    //CHECKSTYLE:OFF
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            final ServerCall<ReqT, RespT> call,
            final Metadata headers,
            final ServerCallHandler<ReqT, RespT> next
    ) {
        String xid = headers.get(SeataGrpcConstant.XID_METADATA_KEY);
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        if (!StringUtils.hasText(xid)) {
            return listener;
        }
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            /**
             * 接收请求消息.
             *
             * @param message 请求消息
             */
            @Override
            public void onMessage(final ReqT message) {
                runWithXid(xid, () -> super.onMessage(message));
            }

            /**
             * 半关闭回调.
             */
            @Override
            public void onHalfClose() {
                runWithXid(xid, super::onHalfClose);
            }

            /**
             * 取消回调.
             */
            @Override
            public void onCancel() {
                runWithXid(xid, super::onCancel);
            }

            /**
             * 完成回调.
             */
            @Override
            public void onComplete() {
                runWithXid(xid, super::onComplete);
            }

            /**
             * 就绪回调.
             */
            @Override
            public void onReady() {
                runWithXid(xid, super::onReady);
            }
        };
    }
    //CHECKSTYLE:ON

    /**
     * 在指定Seata全局事务XID上下文中执行逻辑.
     *
     * @param xid      Seata全局事务XID
     * @param runnable 待执行逻辑
     */
    private void runWithXid(final String xid, final Runnable runnable) {
        String previousXid = RootContext.getXID();
        try {
            // Grpc回调线程可能被复用，执行前先绑定本次请求透传过来的XID。
            RootContext.bind(xid);
            runnable.run();
        } finally {
            // 执行完成后恢复线程原有XID，避免污染后续请求。
            RootContext.unbind();
            if (StringUtils.hasText(previousXid)) {
                RootContext.bind(previousXid);
            }
        }
    }
}
