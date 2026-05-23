package com.oriole.wisepen.common.config;

import com.oriole.wisepen.common.swagger.ApiEnumRegistry;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class WisepenOpenApiAutoConfiguration {

    static {
        ModelResolver.enumsAsRef = true;
    }

    @Bean
    public OpenAPI wisepenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WisePen Cloud API")
                        .version("1.0.0")
                        .description("WisePen 后端公开接口文档"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .pathsToExclude("/internal/**")
                .build();
    }

    @Bean
    public OpenApiCustomizer apiEnumOpenApiCustomizer(ApiEnumRegistry registry) {
        return registry::customize;
    }

    @Bean
    public ApiEnumRegistry apiEnumRegistry() {
        return new ApiEnumRegistry();
    }
}
