package com.oriole.wisepen.common.swagger;

import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ApiEnumRegistry {

    private static final String BASE_PACKAGE = "com.oriole.wisepen";

    public void customize(OpenAPI openApi) {
        Map<String, Schema> schemas = Optional.ofNullable(openApi.getComponents())
                .map(components -> components.getSchemas())
                .orElse(Map.of());

        findApiEnums().forEach(enumClass -> {
            Schema schema = schemas.get(enumClass.getSimpleName());
            if (schema == null) {
                return;
            }

            List<ApiEnumItem> items = Arrays.stream(enumClass.getEnumConstants())
                    .map(item -> (WisePenEnum) item)
                    .map(item -> new ApiEnumItem(item.getKey(), item.getCode(), item.getValue(), item.getDesc()))
                    .toList();

            schema.setEnum(items.stream().map(ApiEnumItem::value).toList());
            schema.addExtension("x-enum-items", items);
            schema.addExtension("x-enum-varnames", items.stream().map(ApiEnumItem::key).toList());
            schema.addExtension("x-enum-descriptions", items.stream().map(ApiEnumItem::desc).toList());
        });
    }

    private List<Class<? extends WisePenEnum>> findApiEnums() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(WisePenEnum.class));

        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(bean -> ClassUtils.resolveClassName(
                        Objects.requireNonNull(bean.getBeanClassName()),
                        ClassUtils.getDefaultClassLoader()))
                .filter(Class::isEnum)
                .map(this::asWisePenEnumClass)
                .toList();
    }

    private Class<? extends WisePenEnum> asWisePenEnumClass(Class<?> clazz) {
        return clazz.asSubclass(WisePenEnum.class);
    }
}
