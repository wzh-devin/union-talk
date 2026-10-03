package com.devin.uniontalk.datasource.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/5/12 01:59.
 *
 * <p></p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.postgresql")
public class PostgresqlProperties {

    /**
     * 数据库主机.
     */
    private String host = "localhost";

    /**
     * 数据库端口.
     */
    private int port = 5432;

    /**
     * 数据库名称.
     */
    private String database = "postgres";

    /**
     * 数据库模式.
     */
    private String schema = "public";

    /**
     * 数据库用户名.
     */
    private String username = "postgres";

    /**
     * 数据库密码.
     */
    private String password;
}
