package com.devin.uniontalk.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 2026/5/16 22:00.
 *
 * <p>
 * Sa-Token 配置类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnWebApplication
public class SaTokenConfiguration implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器，开启路由鉴权.
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/ws/**");
    }
}
