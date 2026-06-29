package com.project.sentic.infra.google;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GoogleClient {

    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final WebClient webClient = WebClient.create();

    public GoogleUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri(USER_INFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("구글 사용자 정보 조회 실패: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED)))
                )
                .bodyToMono(GoogleUserInfo.class)
                .block();
    }
}
