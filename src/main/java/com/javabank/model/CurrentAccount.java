package com.javabank.model;

import com.javabank.exception.AccountFrozenException;
import com.javabank.exception.BankException;
import com.javabank.exception.InsufficientBalanceException;
import java.math.BigDecimal;

public class CurrentAccount extends Account {
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("10000.00");
    public static final BigDecimal MIN_BALANCE = BigDecimal.ZERO;

    public CurrentAccount(String accNo, Customer customer, BigDecimal initialDeposit) {
        super(accNo, customer, initialDeposit);
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws BankException {
        if (getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Account is not active.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Invalid withdrawal amount.");
        }

        // Available funds include current balance + overdraft allowance
        BigDecimal availableFunds = balance.add(OVERDRAFT_LIMIT);
        if (amount.compareTo(availableFunds) > 0) {
            throw new InsufficientBalanceException(availableFunds, amount);
        }

        this.balance = this.balance.subtract(amount);
        recordTransaction(TransactionType.WITHDRAWAL, amount, "Commercial Withdrawal / Overdraft");
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }
}
