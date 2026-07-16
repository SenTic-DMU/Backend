package com.project.sentic.infra.toss;

import lombok.Getter;

@Getter
public class TossConfirmResponse {

    private String paymentKey;
    private String orderId;
    private String method;
    private String status;
}
