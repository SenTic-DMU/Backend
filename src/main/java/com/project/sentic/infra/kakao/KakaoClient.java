package com.project.sentic.infra.kakao;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class KakaoClient {

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient = WebClient.create();

    public KakaoUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri(USER_INFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("카카오 사용자 정보 조회 실패: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED)))
                )
                .bodyToMono(KakaoUserInfo.class)
                .block();
    }
}
