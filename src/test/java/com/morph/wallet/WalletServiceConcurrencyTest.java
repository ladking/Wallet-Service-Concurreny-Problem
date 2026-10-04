package com.morph.wallet;

import com.morph.wallet.dto.WalletDTO;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class WalletServiceConcurrencyTest {
    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;
    @Autowired IdempotencyRepository idempotencyRepository;

    private Long testWalletId;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        idempotencyRepository.deleteAll();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("50.00")); // Initial balance $50.00
        Wallet savedWallet = walletRepository.save(wallet);

        testWalletId = savedWallet.getId();
    }

    @Test
    @DisplayName("Concurrent payments should not cause double-spending or negative balances")
    void testConcurrentPayments_PreventDoubleSpend() throws InterruptedException {
        int numberOfThreads = 10;
        BigDecimal deductionAmount = new BigDecimal("20.00"); // 10 x $20 ($200 total attempted against $50)

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // Latch 1: Holds back all threads until ready so they fire at the exact same millisecond
        CountDownLatch startLatch = new CountDownLatch(1);

        // Latch 2: Blocks the main test thread until all concurrent tasks complete execution
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulDeductions = new AtomicInteger(0);
        AtomicInteger failedDeductions = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            String idempotencyKey = "payment-" + i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal from main thread
                        WalletDTO.PaymentResponse response = walletService.processPayment(testWalletId, deductionAmount, idempotencyKey);
                    if (response.success()) {
                        successfulDeductions.incrementAndGet();
                    } else {
                        failedDeductions.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Catch locking failures, optimstic lock exceptions, or timeouts
                    failedDeductions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all 10 worker threads simultaneously
        startLatch.countDown();

        // Wait for all 10 requests to settle
        doneLatch.await();
        executorService.shutdown();

        // Fetch fresh state directly from DB
        Wallet updatedWallet = walletRepository.findById(testWalletId)
                .orElseThrow(() -> new IllegalStateException("Wallet missing after test execution"));

        System.out.println("--- CONCURRENCY TEST RESULTS ---");
        System.out.println("Final DB Balance: $" + updatedWallet.getBalance());
        System.out.println("Successful Txns: " + successfulDeductions.get());
        System.out.println("Failed/Rejected Txns: " + failedDeductions.get());

        // Assertions for correctness:
        // 1. Balance must NEVER be negative.
        assertTrue(
                updatedWallet.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "CRITICAL: Wallet balance became negative! Race condition vulnerability detected."
        );

        // 2. Exactly 2 payments of $20 should succeed on a $50 balance (total $40 spent, leaving $10).
        assertEquals(2, successfulDeductions.get(), "Only 2 deductions should succeed.");
        assertEquals(8, failedDeductions.get(), "8 deductions should be rejected due to insufficient balance or locks.");
        assertEquals(0, new BigDecimal("10.00").compareTo(updatedWallet.getBalance()), "Final balance must equal $10.00.");
    }

}
