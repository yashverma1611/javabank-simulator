package com.javabank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Transaction {
    private final String txnId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public Transaction(String txnId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter, LocalDateTime timestamp, String description) {
        this.txnId = txnId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.description = description;
    }

    public String getTxnId() { return txnId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format("[%s] %-12s | Amount: ₹%-10.2f | Balance: ₹%-10.2f | %s (%s)",
                txnId, type, amount, balanceAfter, description, timestamp.format(fmt));
    }
}
