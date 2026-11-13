package com.javabank;

import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.service.BankService;
import com.javabank.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class BankServiceTest {
    private BankService bankService;
    private TransactionService txnService;

    @BeforeEach
    public void setup() {
        bankService = BankService.getInstance();
        txnService = new TransactionService(bankService.getRepository());
    }

    @Test
    public void testSavingsAccountCreationAndMinimumBalance() throws BankException {
        Account acc = bankService.createAccount("Test User", "9999999999", "test@bank.com", "1234", AccountType.SAVINGS, new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("2000.00"), acc.getBalance());

        // Attempting to withdraw below ₹1,000 should throw InsufficientBalanceException
        assertThrows(InsufficientBalanceException.class, () -> {
            acc.withdraw(new BigDecimal("1500.00"));
        });
    }

    @Test
    public void testCurrentAccountOverdraftAllowance() throws BankException {
        Account acc = bankService.createAccount("Business Merchant", "8888888888", "corp@bank.com", "5555", AccountType.CURRENT, new BigDecimal("5000.00"));
        
        // Allowed to withdraw beyond balance up to ₹10,000 overdraft
        acc.withdraw(new BigDecimal("12000.00"));
        assertEquals(new BigDecimal("-7000.00"), acc.getBalance());

        // Withdrawing beyond ₹10,000 overdraft should fail
        assertThrows(InsufficientBalanceException.class, () -> {
            acc.withdraw(new BigDecimal("4000.00"));
        });
    }

    @Test
    public void testPinLockoutAfterThreeAttempts() throws BankException {
        Account acc = bankService.createAccount("Lockout Target", "7777777777", "lock@bank.com", "4321", AccountType.SAVINGS, new BigDecimal("3000.00"));
        Customer c = acc.getCustomer();

        // Attempts 1 and 2 throw InvalidPinException
        assertThrows(InvalidPinException.class, () -> c.validatePin("0000"));
        assertThrows(InvalidPinException.class, () -> c.validatePin("1111"));

        // Attempt 3 triggers lockout (AccountFrozenException)
        assertThrows(AccountFrozenException.class, () -> c.validatePin("2222"));
        assertTrue(c.isLocked());
    }

    @Test
    public void testAtomicTransferWithRollback() throws BankException {
        Account src = bankService.createAccount("Source Customer", "6666666666", "src@bank.com", "1234", AccountType.SAVINGS, new BigDecimal("10000.00"));
        Account dest = bankService.createAccount("Dest Customer", "5555555555", "dest@bank.com", "1234", AccountType.SAVINGS, new BigDecimal("5000.00"));

        dest.setStatus(AccountStatus.FROZEN);

        // Transfer should fail and source funds must remain unchanged
        assertThrows(BankException.class, () -> {
            txnService.transferFunds(src.getAccountNumber(), dest.getAccountNumber(), new BigDecimal("2000.00"), "1234");
        });

        assertEquals(new BigDecimal("10000.00"), src.getBalance());
    }
}
