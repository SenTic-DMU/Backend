package com.project.sentic.global.exception;

import lombok.Getter;

/**
 * 커스텀 예외 클래스
 * 
 * 우리가 직접 정의한 에러 상황에서 던지는 예외예요.
 * ErrorCode를 받아서 어떤 에러인지 담아서 던집니다.
 * 
 * 사용 예시:
 * throw new CustomException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
