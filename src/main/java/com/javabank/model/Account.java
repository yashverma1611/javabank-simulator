package com.javabank.model;

import com.javabank.exception.AccountFrozenException;
import com.javabank.exception.BankException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Account {
    private final String accountNumber;
    private final Customer customer;
    protected BigDecimal balance;
    private AccountStatus status;
    private final List<Transaction> transactionHistory = new ArrayList<>();

    public Account(String accountNumber, Customer customer, BigDecimal initialDeposit) {
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.balance = initialDeposit;
        this.status = AccountStatus.ACTIVE;
    }

    public synchronized void deposit(BigDecimal amount) throws BankException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Deposit amount must be strictly positive.");
        }
        this.balance = this.balance.add(amount);
        recordTransaction(TransactionType.DEPOSIT, amount, "Cash Deposit");
    }

    // Abstract method enforcing polymorphic withdrawal rules in subclasses
    public abstract void withdraw(BigDecimal amount) throws BankException;

    public void recordTransaction(TransactionType type, BigDecimal amount, String desc) {
        Transaction txn = new Transaction(type, amount, this.balance, desc);
        transactionHistory.add(txn);
    }

    public void addHistoricalTransaction(Transaction txn) {
        transactionHistory.add(txn);
    }

    // Encapsulation: Getter methods with no direct balance setters
    public String getAccountNumber() { return accountNumber; }
    public Customer getCustomer() { return customer; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }
    public abstract AccountType getAccountType();
}
