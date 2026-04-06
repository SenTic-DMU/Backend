package com.project.sentic.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정
 *
 * API 명세서를 자동으로 생성해주는 Swagger를 설정해요.
 * JWT 토큰 인증도 Swagger UI에서 바로 테스트할 수 있게 설정합니다.
 *
 * 접속 주소: http://localhost:8080/swagger-ui.html
 * 프론트팀에게 이 주소 공유하면 API 명세서로 활용 가능해요.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "JWT";

        // Swagger UI 상단에 Authorize 버튼 추가 (JWT 토큰 입력용)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtScheme);

        // Bearer 토큰 방식으로 JWT 인증 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new OpenAPI()
                .info(new Info()
                        .title("SenTic API")
                        .description("AI 영어 소통 학습 앱 백엔드 API 명세서")
                        .version("v1.0"))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes(jwtScheme, securityScheme));
    }
}
