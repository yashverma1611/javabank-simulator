package com.javabank.repository;

import com.javabank.model.Account;
import java.util.Collection;
import java.util.Optional;

public interface AccountRepository {
    void save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
    Collection<Account> findAll();
    void delete(String accountNumber);
}
