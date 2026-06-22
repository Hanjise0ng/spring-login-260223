package com.han.back.global.config;

import com.han.back.global.docs.ApiErrorCodeWriter;
import com.han.back.global.response.ResponseStatus;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class CommonErrorResponseDocsConfig {

    @Bean
    public OpenApiCustomizer commonErrorResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        ApiErrorCodeWriter.write(operation.getResponses(),
                                ResponseStatus.INTERNAL_SERVER_ERROR, null);
                        ApiErrorCodeWriter.write(operation.getResponses(),
                                ResponseStatus.DB_ERROR, null);
                    }));
        };
    }

}