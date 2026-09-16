package com.project.sentic.infra.kakao;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class KakaoClient {

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    @Value("${oauth2.kakao.client-id}")
    private String clientId;

    @Value("${oauth2.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth2.kakao.redirect-uri:https://localhost/kakao}")
    private String redirectUri;

    private final WebClient webClient = WebClient.create();

    // code로 accessToken 교환
    public String getAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }

        KakaoTokenResponse response = webClient.post()
                .uri(TOKEN_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("카카오 토큰 교환 실패: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED)))
                )
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        return response.getAccessToken();
    }

    // accessToken으로 사용자 정보 조회
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