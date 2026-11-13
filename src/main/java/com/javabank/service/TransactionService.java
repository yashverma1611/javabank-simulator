package com.javabank.service;

import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.repository.AccountRepository;
import java.math.BigDecimal;

public class TransactionService {
    private final AccountRepository accountRepository;

    public TransactionService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Atomic Fund Transfer with Two-Phase Compensating Rollback
    public synchronized String transferFunds(String sourceAccNo, String targetAccNo, BigDecimal amount, String enteredPin)
            throws BankException {

        if (sourceAccNo.equalsIgnoreCase(targetAccNo)) {
            throw new BankException("Source and target account cannot be identical.");
        }

        Account source = accountRepository.findByAccountNumber(sourceAccNo)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAccNo));
        Account target = accountRepository.findByAccountNumber(targetAccNo)
                .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + targetAccNo));

        // Validate PIN and Account Status
        source.getCustomer().validatePin(enteredPin);
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Source account is frozen or inactive.");
        }
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Target account is frozen or inactive.");
        }

        // Phase 1: Debit Source Account
        source.withdraw(amount);
        source.recordTransaction(TransactionType.TRANSFER_OUT, amount, "Transfer to " + targetAccNo);

        // Phase 2: Credit Target Account with Rollback Guard
        try {
            target.deposit(amount);
            target.recordTransaction(TransactionType.TRANSFER_IN, amount, "Transfer from " + sourceAccNo);
        } catch (Exception ex) {
            // Compensating Transaction: Rollback debit if credit fails
            source.deposit(amount);
            source.recordTransaction(TransactionType.ROLLBACK, amount, "Transfer Rollback Reversal");
            accountRepository.save(source);
            throw new BankException("Transfer failed during target credit. Funds safely restored: " + ex.getMessage(), ex);
        }

        // Persist both accounts to disk atomically
        accountRepository.save(source);
        accountRepository.save(target);

        return "TXN-" + System.currentTimeMillis();
    }
}
