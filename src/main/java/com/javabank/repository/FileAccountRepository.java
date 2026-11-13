package com.javabank.repository;

import com.javabank.model.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class FileAccountRepository implements AccountRepository {
    private static final String DATA_DIR = "data";
    private static final String ACCOUNTS_FILE = "data/accounts.csv";
    private static final String TRANSACTIONS_FILE = "data/transactions.csv";
    private final Map<String, Account> accountMap = new HashMap<>();

    public FileAccountRepository() {
        loadAccountsFromDisk();
    }

    @Override
    public synchronized void save(Account account) {
        accountMap.put(account.getAccountNumber(), account);
        flushAccountsToDisk();
        flushTransactionsToDisk(account);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accNo) {
        return Optional.ofNullable(accountMap.get(accNo));
    }

    @Override
    public Collection<Account> findAll() {
        return Collections.unmodifiableCollection(accountMap.values());
    }

    @Override
    public synchronized void delete(String accountNumber) {
        accountMap.remove(accountNumber);
        flushAccountsToDisk();
    }

    private void loadAccountsFromDisk() {
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // accNo, type, balance, custName, phone, email, pinHash, status, failedAttempts, isLocked
                String[] parts = line.split(",");
                if (parts.length >= 8) {
                    String accNo = parts[0].trim();
                    AccountType type = AccountType.valueOf(parts[1].trim());
                    BigDecimal balance = new BigDecimal(parts[2].trim());
                    String custName = parts[3].trim();
                    String phone = parts[4].trim();
                    String email = parts[5].trim();
                    String pinHash = parts[6].trim();
                    AccountStatus status = AccountStatus.valueOf(parts[7].trim());
                    int failedAttempts = parts.length > 8 ? Integer.parseInt(parts[8].trim()) : 0;
                    boolean isLocked = parts.length > 9 ? Boolean.parseBoolean(parts[9].trim()) : false;

                    Customer customer = new Customer("CUST-" + accNo, custName, phone, email, pinHash, failedAttempts, isLocked);
                    Account account;
                    if (type == AccountType.SAVINGS) {
                        account = new SavingsAccount(accNo, customer, balance);
                    } else {
                        account = new CurrentAccount(accNo, customer, balance);
                    }
                    account.setStatus(status);
                    accountMap.put(accNo, account);
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error loading accounts from disk: " + e.getMessage());
        }
    }

    private synchronized void flushAccountsToDisk() {
        new File(DATA_DIR).mkdirs();
        File tmpFile = new File(ACCOUNTS_FILE + ".tmp");
        File destFile = new File(ACCOUNTS_FILE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile))) {
            for (Account acc : accountMap.values()) {
                Customer c = acc.getCustomer();
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%d,%b%n",
                        acc.getAccountNumber(),
                        acc.getAccountType(),
                        acc.getBalance().toPlainString(),
                        c.getName(),
                        c.getPhone(),
                        c.getEmail(),
                        c.getPinHash(),
                        acc.getStatus(),
                        c.getFailedAttempts(),
                        c.isLocked()));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write temporary accounts file: " + e.getMessage());
            return;
        }

        // Atomic file replacement
        if (destFile.exists()) {
            destFile.delete();
        }
        tmpFile.renameTo(destFile);
    }

    private synchronized void flushTransactionsToDisk(Account account) {
        new File(DATA_DIR).mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            List<Transaction> txns = account.getTransactionHistory();
            if (!txns.isEmpty()) {
                Transaction latest = txns.get(txns.size() - 1);
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s%n",
                        latest.getTxnId(),
                        account.getAccountNumber(),
                        latest.getType(),
                        latest.getAmount().toPlainString(),
                        latest.getBalanceAfter().toPlainString(),
                        latest.getTimestamp(),
                        latest.getDescription().replace(",", ";")));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to append transaction log: " + e.getMessage());
        }
    }
}
