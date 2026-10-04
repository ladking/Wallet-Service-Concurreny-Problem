package com.morph.wallet.dto;

import java.math.BigDecimal;

import com.morph.wallet.entity.TransactionStatus;

public class WalletDTO {
    
    public static record Pay(
        BigDecimal amount,
        Long walletId
    ){}


    public static  record PaymentResponse(
        boolean success,
        TransactionStatus status,
        String message
    ){}


    public static enum RESP_MESSAGES{
        ERR_ISSUFICIENT_FUND("insufficient fund"), 
        ERR_DUPLICATE_TRANSACTION("duplicate transaction"),
        ERR_WALLET_NOT_FOUND("wallet not found"),
        ERR_SERVER_ERROR("server error"),
        ERR_INVALID_AMOUNT("invalid amount"),

        TRANS_SUCCESS("transaction success"),
        TRANS_PENDING("transaction pending");


        private final String value;
        RESP_MESSAGES(String value){
            this.value = value;
        }

        public String getValue(){
            return this.value;
        }
    }
}
