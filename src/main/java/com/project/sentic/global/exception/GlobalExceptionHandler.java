package com.project.sentic.global.exception;

import com.project.sentic.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 *
 * 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리해요.
 * 예외 종류에 따라 적절한 HTTP 상태코드와 메시지를 반환합니다.
 *
 * @RestControllerAdvice: 모든 컨트롤러에 적용되는 예외 처리기
 * @ExceptionHandler: 특정 예외 타입을 처리하는 메서드
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 우리가 직접 던지는 CustomException 처리
     * 예: throw new CustomException(ErrorCode.USER_NOT_FOUND)
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        log.warn("CustomException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    /**
     * @Valid 어노테이션으로 DTO 유효성 검사 실패 시 처리
     * 예: 이메일 형식 오류, 필수 입력값 누락 등
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("ValidationException: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail("입력값을 확인해주세요."));
    }

    /**
     * 예상치 못한 서버 에러 처리
     * 위에서 처리되지 않은 모든 예외가 여기로 옴
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("UnhandledException: ", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
