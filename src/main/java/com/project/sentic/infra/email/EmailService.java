package com.project.sentic.infra.email;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[SenTic] 비밀번호 재설정 인증코드");
        message.setText(
                "안녕하세요, SenTic입니다.\n\n" +
                "비밀번호 재설정 인증코드: " + code + "\n\n" +
                "인증코드는 5분간 유효합니다.\n" +
                "본인이 요청하지 않은 경우 이 메일을 무시해 주세요."
        );
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
