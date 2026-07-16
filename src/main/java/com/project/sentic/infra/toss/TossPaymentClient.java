package com.project.sentic.infra.toss;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.create();

    public TossConfirmResponse confirm(String paymentKey, String orderId, Integer amount) {
        String credentials = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        return webClient.post()
                .uri(CONFIRM_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("토스페이먼츠 승인 API 오류: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.PAYMENT_FAILED)))
                )
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }
}
