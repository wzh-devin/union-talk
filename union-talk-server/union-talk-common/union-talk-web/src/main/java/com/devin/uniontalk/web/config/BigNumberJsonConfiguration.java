package com.devin.uniontalk.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 2026/5/30 19:06.
 *
 * <p>
 * 大数字Json转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class BigNumberJsonConfiguration {

    /**
     * 创建自定义转换器.
     *
     * @return 自定义转换器
     */
    @Bean("jackson2ObjectMapperBuilderCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return new Jackson2ObjectMapperBuilderCustomizer() {
            @Override
            public void customize(final Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
                jacksonObjectMapperBuilder
                        .serializerByType(Long.class, ToStringSerializer.instance)
                        .serializerByType(Long.TYPE, ToStringSerializer.instance)
                        .serializerByType(BigInteger.class, ToStringSerializer.instance)
                        .serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                            @Override
                            public void serialize(final BigDecimal value, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
                                jsonGenerator.writeString(value.stripTrailingZeros().toPlainString());
                            }
                        });
            }
        };
    }
}
