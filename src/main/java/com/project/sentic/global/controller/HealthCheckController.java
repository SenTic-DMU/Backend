package com.project.sentic.global.controller;

import com.project.sentic.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스체크 컨트롤러
 *
 * 서버가 정상적으로 동작하는지 확인하는 API예요.
 * Cloudtype 배포 시 헬스체크 엔드포인트로도 사용됩니다.
 *
 * GET /health → 서버 정상 동작 확인
 */
@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("서버가 정상 동작 중입니다.");
    }
}
