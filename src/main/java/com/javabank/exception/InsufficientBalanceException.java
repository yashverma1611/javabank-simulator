package com.javabank.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BankException {
    private final BigDecimal currentBalance;
    private final BigDecimal attemptedAmount;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal attemptedAmount) {
        super(String.format("Insufficient funds! Available: ₹%.2f, Attempted: ₹%.2f", currentBalance, attemptedAmount));
        this.currentBalance = currentBalance;
        this.attemptedAmount = attemptedAmount;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getAttemptedAmount() { return attemptedAmount; }
}
