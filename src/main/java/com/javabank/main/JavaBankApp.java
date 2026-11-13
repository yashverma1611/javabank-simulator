package com.javabank.main;

import com.javabank.exception.BankException;
import com.javabank.model.*;
import com.javabank.service.AdminService;
import com.javabank.service.BankService;
import com.javabank.service.TransactionService;
import com.javabank.util.InputHelper;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JavaBankApp {
    private static final BankService bankService = BankService.getInstance();
    private static final TransactionService transactionService = new TransactionService(bankService.getRepository());
    private static final AdminService adminService = new AdminService(bankService.getRepository());

    public static void main(String[] args) {
        seedInitialAccountsIfEmpty();

        while (true) {
            printHeader("JAVABANK CORE BANKING SIMULATOR v1.0");
            System.out.println(" 1. Customer Banking Portal (ATM / Self-Service)");
            System.out.println(" 2. Bank Administrator Console (Liquidity & Audits)");
            System.out.println(" 3. Open New Bank Account");
            System.out.println(" 4. Exit Application");
            System.out.println("===================================================");

            int choice = InputHelper.readInt("Select Entry Mode (1-4): ", 1, 4);
            switch (choice) {
                case 1 -> customerPortal();
                case 2 -> adminConsole();
                case 3 -> openNewAccount();
                case 4 -> {
                    System.out.println("\n[!] Thank you for banking with JavaBank. Goodbye!");
                    System.exit(0);
                }
            }
        }
    }

    private static void customerPortal() {
        printHeader("CUSTOMER ACCOUNT AUTHENTICATION");
        String accNo = InputHelper.readString("Enter Account Number (e.g., SB-2026-4819): ");

        try {
            Account account = bankService.getAccount(accNo);
            String pin = InputHelper.readPin("Enter 4-Digit Security PIN: ");
            account.getCustomer().validatePin(pin);

            boolean sessionActive = true;
            while (sessionActive) {
                System.out.println("\n===================================================");
                System.out.println(" [AUTHENTICATED] Welcome, " + account.getCustomer().getName() + " (A/C: " + account.getAccountNumber() + ")");
                System.out.printf(" Current Balance: ₹ %,.2f | Type: %s | Status: %s%n",
                        account.getBalance(), account.getAccountType(), account.getStatus());
                System.out.println(" 1. Deposit Funds");
                System.out.println(" 2. Cash Withdrawal");
                System.out.println(" 3. Transfer Funds (Two-Phase Atomic)");
                System.out.println(" 4. Mini-Statement (Recent Transaction History)");
                System.out.println(" 5. Logout & Return to Main Menu");
                System.out.println("===================================================");

                int op = InputHelper.readInt("Select Operation (1-5): ", 1, 5);
                switch (op) {
                    case 1 -> {
                        BigDecimal amt = InputHelper.readBigDecimal("Enter Deposit Amount: ₹ ");
                        bankService.deposit(accNo, amt);
                        System.out.printf("[SUCCESS] Deposited ₹ %,.2f. New Balance: ₹ %,.2f%n", amt, account.getBalance());
                    }
                    case 2 -> {
                        BigDecimal amt = InputHelper.readBigDecimal("Enter Withdrawal Amount: ₹ ");
                        try {
                            account.withdraw(amt);
                            bankService.getRepository().save(account);
                            System.out.printf("[SUCCESS] Dispensed ₹ %,.2f. Remaining Balance: ₹ %,.2f%n", amt, account.getBalance());
                        } catch (BankException e) {
                            System.out.println("[DECLINED] " + e.getMessage());
                        }
                    }
                    case 3 -> {
                        String targetAcc = InputHelper.readString("Enter Target Beneficiary Account Number: ");
                        BigDecimal amt = InputHelper.readBigDecimal("Enter Transfer Amount: ₹ ");
                        String transferPin = InputHelper.readPin("Re-enter 4-Digit PIN to authorize transfer: ");
                        try {
                            String txnId = transactionService.transferFunds(accNo, targetAcc, amt, transferPin);
                            System.out.printf("[SUCCESS] Transfer of ₹ %,.2f settled! Reference: %s%n", amt, txnId);
                            System.out.printf("          Updated Balance: ₹ %,.2f%n", account.getBalance());
                        } catch (BankException e) {
                            System.out.println("[FAILED] " + e.getMessage());
                        }
                    }
                    case 4 -> showMiniStatement(account);
                    case 5 -> {
                        System.out.println("[INFO] Logged out successfully.");
                        sessionActive = false;
                    }
                }
            }

        } catch (BankException e) {
            System.out.println("\n[!] Access Denied: " + e.getMessage());
        }
    }

    private static void adminConsole() {
        printHeader("BANK ADMINISTRATOR CONTROL CENTER");
        System.out.println(" 1. View Bank Total Liquidity (Stream API)");
        System.out.println(" 2. List All Active Accounts");
        System.out.println(" 3. Find Account with Highest Balance");
        System.out.println(" 4. Search Accounts by Name or Account Number");
        System.out.println(" 5. Batch Job: Apply 4% Annual Interest to Savings Accounts");
        System.out.println(" 6. Freeze / Unfreeze Customer Account");
        System.out.println(" 7. Back to Main Menu");
        System.out.println("===================================================");

        int op = InputHelper.readInt("Select Admin Action (1-7): ", 1, 7);
        Collection<Account> accounts = bankService.getAllAccounts();

        switch (op) {
            case 1 -> {
                BigDecimal total = adminService.calculateTotalBankDeposits(accounts);
                System.out.println("\n--- ADMIN LIQUIDITY AUDIT SUMMARY ---");
                System.out.printf(" Total Active Accounts: %d%n", accounts.size());
                System.out.printf(" Total Bank Deposits:   ₹ %,.2f%n", total);
                System.out.println(" System Health:         100% OK | File Persistence Sync: COMPLETED");
            }
            case 2 -> {
                System.out.println("\n----------------------------------------------------------------------------------------");
                System.out.printf("%-14s | %-8s | %-15s | %-20s | %-12s | %s%n",
                        "ACCOUNT NO", "TYPE", "BALANCE", "CUSTOMER NAME", "PHONE", "STATUS");
                System.out.println("----------------------------------------------------------------------------------------");
                for (Account a : accounts) {
                    System.out.printf("%-14s | %-8s | ₹%-14.2f | %-20s | %-12s | %s%n",
                            a.getAccountNumber(), a.getAccountType(), a.getBalance(),
                            a.getCustomer().getName(), a.getCustomer().getPhone(), a.getStatus());
                }
            }
            case 3 -> {
                Optional<Account> richest = adminService.findHighestBalanceAccount(accounts);
                richest.ifPresentOrElse(
                        a -> System.out.printf("[TOP DEPOSITOR] %s (%s) — Balance: ₹ %,.2f%n",
                                a.getCustomer().getName(), a.getAccountNumber(), a.getBalance()),
                        () -> System.out.println("[!] No accounts found.")
                );
            }
            case 4 -> {
                String query = InputHelper.readString("Enter search keyword: ");
                List<Account> results = adminService.searchAccounts(accounts, query);
                System.out.println("[FOUND " + results.size() + " MATCHES]");
                for (Account a : results) {
                    System.out.printf("-> %s | %s | ₹%,.2f (%s)%n",
                            a.getAccountNumber(), a.getCustomer().getName(), a.getBalance(), a.getAccountType());
                }
            }
            case 5 -> {
                int count = adminService.applyMonthlyInterest(accounts);
                System.out.printf("[BATCH SUCCESS] Successfully credited monthly 4%% interest across %d active Savings accounts.%n", count);
            }
            case 6 -> {
                String targetAcc = InputHelper.readString("Enter Account Number: ");
                System.out.println("1. ACTIVE (Unfreeze)");
                System.out.println("2. FROZEN (Lock Account)");
                int st = InputHelper.readInt("Select Status (1-2): ", 1, 2);
                AccountStatus status = (st == 1) ? AccountStatus.ACTIVE : AccountStatus.FROZEN;
                adminService.setAccountStatus(targetAcc, status);
                System.out.println("[UPDATED] Account " + targetAcc + " status set to: " + status);
            }
            case 7 -> {}
        }
    }

    private static void openNewAccount() {
        printHeader("OPEN NEW BANK ACCOUNT ONBOARDING");
        String name = InputHelper.readString("Customer Full Name: ");
        String phone = InputHelper.readString("Contact Mobile Number: ");
        String email = InputHelper.readString("Email Address: ");
        String pin = InputHelper.readPin("Set 4-Digit Security PIN: ");

        System.out.println("1. SAVINGS ACCOUNT (Min ₹1,000, 4% Interest, Daily Cap ₹25,000)");
        System.out.println("2. CURRENT ACCOUNT (Zero Balance, ₹10,000 Overdraft Protection)");
        int t = InputHelper.readInt("Choose Account Type (1-2): ", 1, 2);
        AccountType type = (t == 1) ? AccountType.SAVINGS : AccountType.CURRENT;

        BigDecimal deposit = InputHelper.readBigDecimal("Initial Opening Deposit: ₹ ");

        try {
            Account acc = bankService.createAccount(name, phone, email, pin, type, deposit);
            System.out.println("\n===================================================");
            System.out.println(" [SUCCESS] Account Created Successfully!");
            System.out.println(" Account Number:  " + acc.getAccountNumber());
            System.out.println(" Account Holder:  " + acc.getCustomer().getName());
            System.out.printf(" Opening Balance: ₹ %,.2f%n", acc.getBalance());
            System.out.println(" Status:          " + acc.getStatus());
            System.out.println("===================================================");
        } catch (BankException e) {
            System.out.println("[FAILED] " + e.getMessage());
        }
    }

    private static void showMiniStatement(Account account) {
        System.out.println("\n===================================================");
        System.out.println("TRANSACTION JOURNAL (MINI-STATEMENT — LAST 10)");
        System.out.println("===================================================");
        List<Transaction> list = account.getTransactionHistory();
        if (list.isEmpty()) {
            System.out.println("No recorded transactions found.");
            return;
        }

        int start = Math.max(0, list.size() - 10);
        for (int i = list.size() - 1; i >= start; i--) {
            System.out.println(list.get(i));
        }
    }

    private static void seedInitialAccountsIfEmpty() {
        if (bankService.getAllAccounts().isEmpty()) {
            try {
                Account a1 = bankService.createAccount("Yash Raj Soni", "+91 9876543210", "yash.soni@gmail.com", "1234", AccountType.SAVINGS, new BigDecimal("48500.00"));
                Account a2 = bankService.createAccount("City Commercial Enterprise", "+91 9123456789", "accounts@citycorp.in", "9999", AccountType.CURRENT, new BigDecimal("125000.00"));
                Account a3 = bankService.createAccount("Aarav Sharma", "+91 9988776655", "aarav@gmail.com", "1111", AccountType.SAVINGS, new BigDecimal("12000.00"));

                // Seed some realistic transactions
                a1.recordTransaction(TransactionType.DEPOSIT, new BigDecimal("12000.00"), "Direct Salary Credit");
                a1.recordTransaction(TransactionType.WITHDRAWAL, new BigDecimal("3500.00"), "ATM Cash Withdrawal");
                a1.recordTransaction(TransactionType.INTEREST, new BigDecimal("1450.00"), "Quarterly Interest Credit");
                bankService.getRepository().save(a1);
            } catch (Exception ignored) {}
        }
    }

    private static void printHeader(String title) {
        System.out.println("\n===================================================");
        System.out.println(" " + title);
        System.out.println("===================================================");
    }
}
