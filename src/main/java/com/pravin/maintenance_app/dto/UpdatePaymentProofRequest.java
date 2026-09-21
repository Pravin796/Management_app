package com.pravin.maintenance_app.dto;

import lombok.Data;

@Data
public class UpdatePaymentProofRequest {

    private String transactionReference;

    private String screenshotUrl;
}
