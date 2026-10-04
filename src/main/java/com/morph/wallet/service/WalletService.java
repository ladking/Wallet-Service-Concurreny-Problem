package com.morph.wallet.service;

import com.morph.wallet.dto.WalletDTO;
import com.morph.wallet.entity.Wallet;
import com.morph.wallet.entity.IdempotencyKey;
import com.morph.wallet.entity.TransactionStatus;
import com.morph.wallet.repository.IdempotencyRepository;
import com.morph.wallet.repository.WalletRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Transactional
    public WalletDTO.PaymentResponse processPayment(Long walletId, BigDecimal amount, String idempotencyKey) {
        if(amount == null || amount.signum() <= 0){
            return new WalletDTO.PaymentResponse(false, TransactionStatus.FAILED, WalletDTO.RESP_MESSAGES.ERR_INVALID_AMOUNT.getValue());
        }
        // Idempotency check to prevent duplicate transactions, retrieve existing key
         IdempotencyKey idemp_key = idempotencyRepository.findByKey(idempotencyKey).orElse(null);
         if(idemp_key != null){
            if(idemp_key.getTransactionStatus().getValue().equals(TransactionStatus.SUCCESS.getValue())){
                return new WalletDTO.PaymentResponse(true, idemp_key.getTransactionStatus(), idemp_key.getMessage());
            }
            return new WalletDTO.PaymentResponse(false, idemp_key.getTransactionStatus(), idemp_key.getMessage());
         }
             
        idemp_key = new IdempotencyKey(
                    idempotencyKey, 
                    TransactionStatus.PENDING, 
                    WalletDTO.RESP_MESSAGES.TRANS_PENDING.getValue(),
                    walletId
                );
        IdempotencyKey newKey = this.idempotencyRepository.save(idemp_key);
        idemp_key.setId(newKey.getId());

        TransactionStatus status;
        String message;
        boolean isSuccess;
         
        // Aquire lock on wallet to prevent concurrent operation on user wallet and ensure serialized wallet operation
        Wallet wallet = walletRepository.findWalletByIdForUpdate(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) >= 0) {
            // Artificial delay to magnify race condition window during testing
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);

            status = TransactionStatus.SUCCESS;
            isSuccess = true;
            message = WalletDTO.RESP_MESSAGES.TRANS_SUCCESS.getValue();

        }else { 
            status = TransactionStatus.FAILED;
            isSuccess = false;
            message = WalletDTO.RESP_MESSAGES.ERR_ISSUFICIENT_FUND.getValue();
        }

        idemp_key.setTransactionStatus(status);
        idemp_key.setMessage(message);
        this.idempotencyRepository.save(idemp_key);

        return new WalletDTO.PaymentResponse(isSuccess, status, message);
    }
   

}
