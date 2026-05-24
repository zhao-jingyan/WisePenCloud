package com.oriole.wisepen.resource.config;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 仓储与 Jackson 序列化配置。
 * <ul>
 *   <li>显式只扫描 {@code com.oriole.wisepen.resource.repository} 下的 ES 仓储，避免和 MongoDB Repository 互相抢扫描。</li>
 *   <li>注册 {@link JacksonJsonpMapper} 并加入 {@link JavaTimeModule}，让 {@code LocalDateTime} 能正确序列化到 ES。</li>
 * </ul>
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.oriole.wisepen.resource.repository")
public class ESConfiguration {

    /**
     * 用 ES 客户端配套的 Jackson Mapper。
     * <p>
     * 注入 {@link JavaTimeModule} 后，{@code LocalDateTime} 走 ISO 字符串序列化，
     * 与 {@code ESIndexEntity#updateTime} 上配置的 {@code ES_DATE_FORMAT_PATTERN} 匹配。
     */
    @Bean
    @Primary
    public JacksonJsonpMapper jacksonJsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JacksonJsonpMapper(objectMapper);
    }
}
