package com.example.heartbit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        String jwt = "JWT";

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
        Components components = new Components().addSecuritySchemes(jwt, new SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        );
        return new OpenAPI()
                .components(components)
                .addSecurityItem(securityRequirement)
                .info(new Info()
                        .title("heartbit API 명세서")
                        .description("heartbit API 문서입니다.")
                        .version("1.0.0"));

    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            AntPathMatcher pathMatcher = new AntPathMatcher();

            // 1. 스웨거에 등록된 모든 URL 경로(path)를 하나씩 검사
            openApi.getPaths().forEach((path, pathItem) -> {
                boolean isAllowed = false;

                // 2. SecurityConfig에 있는 ALLOWED_URLS 목록과 비교
                for (String allowedUrl : SecurityConfig.ALLOWED_URLS) {
                    // 스웨거 경로(/api/chatroom/{id})와 허용 경로(/api/chatroom/**) 매칭 확인
                    if (pathMatcher.match(allowedUrl, path)) {
                        isAllowed = true;
                        break;
                    }
                }

                // 3. 허용된 경로라면 보안 요구사항(자물쇠) 삭제
                if (isAllowed) {
                    pathItem.readOperations().forEach(operation -> {
                        operation.setSecurity(Collections.emptyList());
                    });
                }
            });
        };
    };
}
