package com.devin.uniontalk.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.gateway.request.TrustedIdentityHttpServletRequest;
import com.devin.uniontalk.web.response.ApiResult;
import com.devin.uniontalk.web.response.ResultEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 2026/07/31 17:10.
 *
 * <p>
 * Agent请求可信用户身份注入过滤器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AgentRequestIdentityFilter extends OncePerRequestFilter {

    /**
     * Agent网关路径.
     */
    private static final String AGENT_PATH = "/agent";

    /**
     * JSON序列化器.
     */
    private final ObjectMapper objectMapper;

    /**
     * 注入已认证用户标识后继续转发Agent请求.
     *
     * @param request     HTTP请求
     * @param response    HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet处理异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(StpUtil.getTokenName());
        if (!StringUtils.hasText(token)) {
            writeUnauthorizedResponse(response);
            return;
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (Objects.isNull(loginId)) {
            writeUnauthorizedResponse(response);
            return;
        }
        TrustedIdentityHttpServletRequest trustedRequest =
                new TrustedIdentityHttpServletRequest(request, loginId.toString());
        filterChain.doFilter(trustedRequest, response);
    }

    /**
     * 判断当前请求是否无需执行身份注入.
     *
     * @param request HTTP请求
     * @return 非Agent请求返回true
     */
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return !AGENT_PATH.equals(servletPath)
                && !servletPath.startsWith(AGENT_PATH + "/");
    }

    /**
     * 写入未登录响应.
     *
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void writeUnauthorizedResponse(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResult.fail(ResultEnum.UNAUTHORIZED)
        );
    }
}
