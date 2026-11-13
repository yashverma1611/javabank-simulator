package com.javabank.model;

import com.javabank.exception.AccountFrozenException;
import com.javabank.exception.BankException;
import com.javabank.exception.InvalidPinException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Customer {
    private final String customerId;
    private final String name;
    private final String phone;
    private final String email;
    private String pinHash;
    private int failedAttempts = 0;
    private boolean isLocked = false;

    public Customer(String customerId, String name, String phone, String email, String plainPin) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.pinHash = hashPin(plainPin);
    }

    public Customer(String customerId, String name, String phone, String email, String pinHash, int failedAttempts, boolean isLocked) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.pinHash = pinHash;
        this.failedAttempts = failedAttempts;
        this.isLocked = isLocked;
    }

    public synchronized void validatePin(String enteredPin) throws BankException {
        if (isLocked) {
            throw new AccountFrozenException("Account is LOCKED due to 3 consecutive wrong PIN attempts.");
        }
        if (!hashPin(enteredPin).equals(this.pinHash)) {
            failedAttempts++;
            if (failedAttempts >= 3) {
                this.isLocked = true;
                throw new AccountFrozenException("Security Alert: 3 incorrect attempts. Account has been LOCKED!");
            }
            throw new InvalidPinException("Invalid PIN! Warning: Attempt " + failedAttempts + " of 3.");
        }
        // Reset failed attempt counter on successful validation
        this.failedAttempts = 0;
    }

    public static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(pin.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing in JVM", e);
        }
    }

    public void unlockAccount() {
        this.isLocked = false;
        this.failedAttempts = 0;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getPinHash() { return pinHash; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return isLocked; }
}
