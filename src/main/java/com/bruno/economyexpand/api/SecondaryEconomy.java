package com.bruno.economyexpand.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public interface SecondaryEconomy {
    Currency getCurrency(String id);
    Collection<Currency> getCurrencies();
    BigDecimal getBalance(UUID player, String currency);
    boolean deposit(UUID player, String currency, BigDecimal amount);
    boolean withdraw(UUID player, String currency, BigDecimal amount);
    boolean setBalance(UUID player, String currency, BigDecimal amount);
    boolean transfer(UUID from, UUID to, String currency, BigDecimal amount);
}