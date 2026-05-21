package com.project.sentic.domain.user.controller;

import com.project.sentic.domain.user.dto.WithdrawRequestDto;
import com.project.sentic.domain.user.service.UserService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 탈퇴", description = "LOCAL 계정은 비밀번호 확인 필수. 소셜 계정은 password 필드 불필요.")
    @DeleteMapping("/me")
    public ApiResponse<String> withdraw(
            @AuthenticationPrincipal Long userId,
            @RequestBody WithdrawRequestDto request) {
        userService.withdraw(userId, request);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }
}
