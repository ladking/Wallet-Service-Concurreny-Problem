package com.morph.wallet.entity;

public enum TransactionStatus {
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed");


    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
