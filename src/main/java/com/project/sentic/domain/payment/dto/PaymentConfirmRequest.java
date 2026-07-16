package com.project.sentic.domain.payment.dto;

import lombok.Getter;

@Getter
public class PaymentConfirmRequest {

    private String paymentKey;
    private String orderId;
    private Integer amount;
    private Integer planId;
}
