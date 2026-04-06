package com.project.sentic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * SenTic 백엔드 메인 클래스
 *
 * @SpringBootApplication: Spring Boot 앱 시작점
 * @EnableJpaAuditing: BaseTimeEntity의 createdAt, updatedAt 자동 관리
 *                     이게 없으면 생성일시/수정일시가 자동으로 안 들어가요!
 */
@SpringBootApplication
@EnableJpaAuditing
public class SenticApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenticApplication.class, args);
    }
}
