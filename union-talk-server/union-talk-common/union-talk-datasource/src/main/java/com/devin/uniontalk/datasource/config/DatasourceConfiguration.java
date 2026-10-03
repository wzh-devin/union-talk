package com.devin.uniontalk.datasource.config;

import com.devin.uniontalk.datasource.properties.PostgresqlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/5/12 02:01.
 *
 * <p></p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(PostgresqlProperties.class)
public class DatasourceConfiguration {
}
