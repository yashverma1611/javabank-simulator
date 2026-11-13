package com.javabank.model;

import com.javabank.exception.AccountFrozenException;
import com.javabank.exception.BankException;
import com.javabank.exception.DailyLimitExceededException;
import com.javabank.exception.InsufficientBalanceException;
import java.math.BigDecimal;

public class SavingsAccount extends Account {
    public static final BigDecimal MIN_BALANCE = new BigDecimal("1000.00");
    public static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("25000.00");
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.04");
    private BigDecimal dailyWithdrawnTotal = BigDecimal.ZERO;

    public SavingsAccount(String accNo, Customer customer, BigDecimal initialDeposit) {
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
        if (dailyWithdrawnTotal.add(amount).compareTo(DAILY_WITHDRAWAL_LIMIT) > 0) {
            throw new DailyLimitExceededException(
                String.format("Daily limit of ₹%.2f exceeded! Already withdrawn: ₹%.2f, Attempted: ₹%.2f",
                        DAILY_WITHDRAWAL_LIMIT, dailyWithdrawnTotal, amount)
            );
        }
        if (balance.subtract(amount).compareTo(MIN_BALANCE) < 0) {
            throw new InsufficientBalanceException(balance, amount);
        }

        this.balance = this.balance.subtract(amount);
        this.dailyWithdrawnTotal = this.dailyWithdrawnTotal.add(amount);
        recordTransaction(TransactionType.WITHDRAWAL, amount, "Cash Withdrawal");
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }
}
