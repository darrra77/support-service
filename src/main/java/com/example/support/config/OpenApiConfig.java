package com.example.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Support Service API")
                .version("1.0.0")
                .description(
                    "REST API для обработки заявок технической поддержки. "
                        + "Разработано на Java, Kotlin и Spring Boot."));
  }
}
