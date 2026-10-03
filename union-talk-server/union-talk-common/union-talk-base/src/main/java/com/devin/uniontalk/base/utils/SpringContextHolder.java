package com.devin.uniontalk.base.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 2026/5/11 22:18.
 *
 * <p>
 * SpringContextHolder
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * 获取bean.
     *
     * @param name bean名称
     * @return bean
     */
    public static Object getBean(final String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 获取bean.
     *
     * @param clazz bean类型
     * @param <T>   bean类型
     * @return bean
     */
    public static <T> T getBean(final Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
