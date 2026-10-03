package com.devin.uniontalk.gateway.request;

import com.devin.uniontalk.gateway.constant.GatewayHeaderConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 2026/07/31 17:10.
 *
 * <p>
 * 仅暴露网关注入用户标识的请求包装器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrustedIdentityHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * 已认证用户标识.
     */
    private final String authenticatedUserId;

    /**
     * 创建可信身份请求包装器.
     *
     * @param request             原始请求
     * @param authenticatedUserId 已认证用户标识
     */
    public TrustedIdentityHttpServletRequest(
            final HttpServletRequest request,
            final String authenticatedUserId
    ) {
        super(request);
        this.authenticatedUserId = authenticatedUserId;
    }

    /**
     * 获取请求头.
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    @Override
    public String getHeader(final String name) {
        if (GatewayHeaderConstant.TRUSTED_USER_ID_HEADER.equalsIgnoreCase(name)) {
            return authenticatedUserId;
        }
        return super.getHeader(name);
    }

    /**
     * 获取同名请求头列表.
     *
     * @param name 请求头名称
     * @return 请求头值枚举
     */
    @Override
    public Enumeration<String> getHeaders(final String name) {
        if (GatewayHeaderConstant.TRUSTED_USER_ID_HEADER.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.singleton(authenticatedUserId));
        }
        return super.getHeaders(name);
    }

    /**
     * 获取请求头名称列表.
     *
     * @return 请求头名称枚举
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNameSet = new LinkedHashSet<>();
        Enumeration<String> headerNames = super.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!GatewayHeaderConstant.TRUSTED_USER_ID_HEADER.equalsIgnoreCase(headerName)) {
                    headerNameSet.add(headerName);
                }
            }
        }
        headerNameSet.add(GatewayHeaderConstant.TRUSTED_USER_ID_HEADER);
        return Collections.enumeration(headerNameSet);
    }
}
