package com.bruno.economyexpand.storage;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletStorage {
    BigDecimal read(UUID uuid, String currency, BigDecimal defaultValue);
    void write(UUID uuid, String currency, BigDecimal value);
    void close();
}