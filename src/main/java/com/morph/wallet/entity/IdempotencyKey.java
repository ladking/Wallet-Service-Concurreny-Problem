package com.morph.wallet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;


@Entity 
@Table( name = "idempotency_keys" )
public class IdempotencyKey {
    @Id 
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "wallet_id", nullable=false)
    private Long walletId;

    @Column(name = "transaction_status", nullable = false)
    private TransactionStatus transactionStatus;

    @Column(name = "response_message")
    private String message;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(
        String idempotencyKey, 
        TransactionStatus transactionStatus,
        String message,
        Long walletId
    ) {
        this.idempotencyKey = idempotencyKey;
        this.transactionStatus = transactionStatus;
        this.message = message;
        this.walletId = walletId;
    }

    public TransactionStatus getTransactionStatus(){
        return this.transactionStatus;
    }

    public String getIdempotencyKey(){
        return this.idempotencyKey;
    }

    public String getMessage(){
        return this.message;
    }

    public Long getId(){
        return this.id;
    }

    public void setMessage(String value){
        this.message = value;
    }

    public void setTransactionStatus(TransactionStatus value){
        this.transactionStatus = value;
    }

    public void setId(Long value){
        this.id = value;
    }
}
