package com.bruno.economyexpand.storage;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletStorage {
    BigDecimal read(UUID uuid, String currency, BigDecimal defaultValue);
    void write(UUID uuid, String currency, BigDecimal value);
    default boolean transfer(UUID from, UUID to, String currency, BigDecimal amount, BigDecimal fromDefault, BigDecimal toDefault) {
        synchronized (this) {
            BigDecimal source = read(from, currency, fromDefault), target = read(to, currency, toDefault);
            if (source.compareTo(amount) < 0) return false;
            write(from, currency, source.subtract(amount));
            try { write(to, currency, target.add(amount)); return true; }
            catch (RuntimeException failure) { try { write(from, currency, source); } catch (RuntimeException ignored) {} return false; }
        }
    }
    void close();
}