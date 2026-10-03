package com.devin.uniontalk.seata.config;

import com.devin.uniontalk.seata.interceptor.SeataGrpcClientInterceptor;
import com.devin.uniontalk.seata.interceptor.SeataGrpcServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 2026/05/20 19:30.
 *
 * <p>
 * Seata Grpc自动配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoConfiguration
public class SeataGrpcConfiguration {

    /**
     * 创建Seata Grpc客户端全局拦截器.
     *
     * @return Grpc客户端拦截器
     */
    @Bean
    @GrpcGlobalClientInterceptor
    public ClientInterceptor seataGrpcClientInterceptor() {
        return new SeataGrpcClientInterceptor();
    }

    /**
     * 创建Seata Grpc服务端全局拦截器.
     *
     * @return Grpc服务端拦截器
     */
    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor seataGrpcServerInterceptor() {
        return new SeataGrpcServerInterceptor();
    }
}
