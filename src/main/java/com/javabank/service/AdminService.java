package com.javabank.service;

import com.javabank.model.*;
import com.javabank.repository.AccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class AdminService {
    private final AccountRepository repository;

    public AdminService(AccountRepository repository) {
        this.repository = repository;
    }

    // Stream API: Calculate total bank deposits across all accounts
    public BigDecimal calculateTotalBankDeposits(Collection<Account> accounts) {
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Stream API: Find account holding the highest balance
    public Optional<Account> findHighestBalanceAccount(Collection<Account> accounts) {
        return accounts.stream()
                .max(Comparator.comparing(Account::getBalance));
    }

    // Stream API: Filter accounts by customer name or account number
    public List<Account> searchAccounts(Collection<Account> accounts, String query) {
        String lowerQuery = query.toLowerCase();
        return accounts.stream()
                .filter(a -> a.getAccountNumber().toLowerCase().contains(lowerQuery) ||
                             a.getCustomer().getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // Batch Job: Apply monthly 4% annual interest to all active savings accounts
    public int applyMonthlyInterest(Collection<Account> accounts) {
        int processedCount = 0;
        BigDecimal monthlyRate = new BigDecimal("0.04").divide(new BigDecimal("12"), 6, RoundingMode.HALF_EVEN);

        for (Account acc : accounts) {
            if (acc instanceof SavingsAccount && acc.getStatus() == AccountStatus.ACTIVE) {
                BigDecimal interest = acc.getBalance().multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
                try {
                    acc.deposit(interest);
                    acc.recordTransaction(TransactionType.INTEREST, interest, "Monthly 4% Interest Credit");
                    repository.save(acc);
                    processedCount++;
                } catch (Exception ignored) {}
            }
        }
        return processedCount;
    }

    public void setAccountStatus(String accNo, AccountStatus status) {
        repository.findByAccountNumber(accNo).ifPresent(acc -> {
            acc.setStatus(status);
            if (status == AccountStatus.ACTIVE) {
                acc.getCustomer().unlockAccount();
            }
            repository.save(acc);
        });
    }
}
