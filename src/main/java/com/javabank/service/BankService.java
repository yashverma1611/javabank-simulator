package com.javabank.service;

import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.repository.AccountRepository;
import com.javabank.repository.FileAccountRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public class BankService {
    private static BankService instance;
    private final AccountRepository repository;

    private BankService() {
        this.repository = new FileAccountRepository();
    }

    public static synchronized BankService getInstance() {
        if (instance == null) {
            instance = new BankService();
        }
        return instance;
    }

    public Account createAccount(String name, String phone, String email, String pin, AccountType type, BigDecimal initialDeposit) throws BankException {
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankException("Initial deposit cannot be negative.");
        }
        if (type == AccountType.SAVINGS && initialDeposit.compareTo(SavingsAccount.MIN_BALANCE) < 0) {
            throw new BankException("Savings account requires an initial minimum deposit of ₹" + SavingsAccount.MIN_BALANCE);
        }

        String prefix = (type == AccountType.SAVINGS) ? "SB-2026-" : "CA-2026-";
        String accNo = prefix + (1000 + repository.findAll().size() + 1);

        Customer customer = new Customer("CUST-" + System.currentTimeMillis() % 10000, name, phone, email, pin);
        Account account;
        if (type == AccountType.SAVINGS) {
            account = new SavingsAccount(accNo, customer, initialDeposit);
        } else {
            account = new CurrentAccount(accNo, customer, initialDeposit);
        }

        account.recordTransaction(TransactionType.DEPOSIT, initialDeposit, "Initial Account Opening Deposit");
        repository.save(account);
        return account;
    }

    public Account getAccount(String accNo) throws AccountNotFoundException {
        return repository.findByAccountNumber(accNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with identifier: " + accNo));
    }

    public void deposit(String accNo, BigDecimal amount) throws BankException {
        Account account = getAccount(accNo);
        account.deposit(amount);
        repository.save(account);
    }

    public void withdraw(String accNo, BigDecimal amount, String pin) throws BankException {
        Account account = getAccount(accNo);
        account.getCustomer().validatePin(pin);
        account.withdraw(amount);
        repository.save(account);
    }

    public Collection<Account> getAllAccounts() {
        return repository.findAll();
    }

    public AccountRepository getRepository() {
        return repository;
    }
}
