package com.morph.wallet;

import com.morph.wallet.dto.WalletDTO;
import com.morph.wallet.entity.IdempotencyKey;
import com.morph.wallet.entity.TransactionStatus;
import com.morph.wallet.entity.Wallet;
import com.morph.wallet.repository.IdempotencyRepository;
import com.morph.wallet.repository.WalletRepository;
import com.morph.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class WalletServiceIdempotencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    private Long testWalletId;

    @BeforeEach
    void setUp() {
	idempotencyRepository.deleteAll();
	walletRepository.deleteAll();

	Wallet wallet = new Wallet();
	wallet.setBalance(new BigDecimal("100.00"));
	testWalletId = walletRepository.save(wallet).getId();
    }

    @Test
    @DisplayName("A repeated idempotency key must not debit the wallet twice")
    void testRepeatedKeyDoesNotProcessPaymentTwice() {
		BigDecimal paymentAmount = new BigDecimal("25.00");
		String idempotencyKey = "payment-123";

		WalletDTO.PaymentResponse firstResponse = walletService.processPayment(
			testWalletId, paymentAmount, idempotencyKey);
		WalletDTO.PaymentResponse repeatedResponse = walletService.processPayment(
			testWalletId, paymentAmount, idempotencyKey);

		Wallet updatedWallet = walletRepository.findById(testWalletId)
			.orElseThrow(() -> new IllegalStateException("Wallet missing after payment"));
		IdempotencyKey savedKey = idempotencyRepository.findByKey(idempotencyKey)
			.orElseThrow(() -> new IllegalStateException("Idempotency key missing after payment"));

		System.out.println("\n\n-----Idempotency Test Result--------");
		System.out.println("Expected wallet balance to be: $75");
		System.out.println("Wallet balance: $" + updatedWallet.getBalance());
		System.out.println("Idempotency Key record status should be updated to success: "+savedKey.getTransactionStatus()+"\n\n");
		System.out.println("First request response: "+firstResponse.message());
		System.out.println("Repeated request response: "+repeatedResponse.message());

		assertTrue(firstResponse.success(), "The first payment should be processed");
		assertEquals(0, new BigDecimal("75.00").compareTo(updatedWallet.getBalance()),"The wallet should be debited only once");
		assertEquals(1, idempotencyRepository.count(), "Only one record should exist for the key");
		assertEquals(TransactionStatus.SUCCESS, savedKey.getTransactionStatus());

    }

	@Test
	@DisplayName("A repeated idempotency key on failed payment")
	void testIdempotencyKeyOnFailedPayment(){

	}
}
