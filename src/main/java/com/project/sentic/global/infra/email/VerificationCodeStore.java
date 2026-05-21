package com.project.sentic.global.infra.email;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {

    private static final int CODE_TTL_MINUTES = 5;
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        store.put(email, new CodeEntry(code, LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES)));
    }

    public void verify(String email, String code) {
        CodeEntry entry = store.get(email);
        if (entry == null) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            store.remove(email);
            throw new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!entry.getCode().equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
    }

    public void remove(String email) {
        store.remove(email);
    }

    @Getter
    @AllArgsConstructor
    private static class CodeEntry {
        private final String code;
        private final LocalDateTime expiresAt;
    }
}
