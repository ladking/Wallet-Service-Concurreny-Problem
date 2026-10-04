package com.morph.wallet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.morph.wallet.dto.WalletDTO;
import com.morph.wallet.service.WalletService;

import java.math.BigDecimal;


@RestController 
@RequestMapping("/api/v1/pay")
public class WalletController {

    @Autowired 
    private final WalletService walletService;

    public WalletController(WalletService walletService){
        this.walletService = walletService;
    }

    @PostMapping("")
    public ResponseEntity<Response> pay(
        @Validated @RequestBody PayDTO reqBody, 
        @RequestHeader(name = "idempotencyKey", required = true) String idempotencyKey
    ){

            WalletDTO.PaymentResponse response = this.walletService.processPayment(reqBody.walletId(), reqBody.amount(), idempotencyKey);
            if(response.success()){
                return new ResponseEntity<>(new Response("success", response), HttpStatus.OK);
            }

            String respMesg = "request failed";
            HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
             
            return new ResponseEntity<>(new Response(respMesg, response), statusCode);
    }
}

record PayDTO(
    BigDecimal amount,
    Long walletId
){}

record Response(
    String message,
    Object data
){}

