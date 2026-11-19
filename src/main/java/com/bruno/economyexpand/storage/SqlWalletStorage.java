package com.bruno.economyexpand.storage;

import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;

public class SqlWalletStorage implements WalletStorage {
    private final Connection connection;

    public SqlWalletStorage(String url, String user, String password) throws SQLException {
        loadDriver(url);
        connection = (user == null || user.trim().isEmpty())
            ? DriverManager.getConnection(url)
            : DriverManager.getConnection(url, user, password == null ? "" : password);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS economyexpand_wallets (player_uuid VARCHAR(36) NOT NULL, currency VARCHAR(64) NOT NULL, balance VARCHAR(64) NOT NULL, PRIMARY KEY(player_uuid,currency))");
        }
    }

    private void loadDriver(String url) throws SQLException {
        String driver;
        if (url.startsWith("jdbc:mysql:")) driver = "com.bruno.economyexpand.libs.mysql.cj.jdbc.Driver";
        else if (url.startsWith("jdbc:sqlite:")) driver = "com.bruno.economyexpand.libs.sqlite.JDBC";
        else if (url.startsWith("jdbc:h2:")) driver = "com.bruno.economyexpand.libs.h2.Driver";
        else throw new SQLException("Unsupported JDBC URL: " + url);
        try {
            Class.forName(driver, true, getClass().getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new SQLException("No se encontró el driver JDBC aislado dentro de EconomyExpandNK: " + driver
                + ". Comprueba que instalaste el JAR sombreado más reciente.", exception);
        }
    }

    public synchronized BigDecimal read(UUID uuid, String currency, BigDecimal fallback) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM economyexpand_wallets WHERE player_uuid=? AND currency=?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, currency.toLowerCase());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new BigDecimal(result.getString(1)) : fallback;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Wallet read failed", exception);
        }
    }

    public synchronized void write(UUID uuid, String currency, BigDecimal value) {
        try {
            connection.setAutoCommit(false);
            upsert(uuid, currency, value);
            connection.commit();
        } catch (SQLException exception) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            throw new IllegalStateException("Wallet write failed", exception);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    public synchronized boolean transfer(UUID from, UUID to, String currency, BigDecimal amount,
                                         BigDecimal fromDefault, BigDecimal toDefault) {
        try {
            connection.setAutoCommit(false);
            BigDecimal source = read(from, currency, fromDefault);
            BigDecimal target = read(to, currency, toDefault);
            if (source.compareTo(amount) < 0) {
                connection.rollback();
                return false;
            }
            upsert(from, currency, source.subtract(amount));
            upsert(to, currency, target.add(amount));
            connection.commit();
            return true;
        } catch (SQLException | RuntimeException exception) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    private void upsert(UUID uuid, String currency, BigDecimal value) throws SQLException {
        boolean exists;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM economyexpand_wallets WHERE player_uuid=? AND currency=?")) {
            query.setString(1, uuid.toString());
            query.setString(2, currency.toLowerCase());
            try (ResultSet result = query.executeQuery()) { exists = result.next(); }
        }
        if (exists) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE economyexpand_wallets SET balance=? WHERE player_uuid=? AND currency=?")) {
                update.setString(1, value.toPlainString());
                update.setString(2, uuid.toString());
                update.setString(3, currency.toLowerCase());
                update.executeUpdate();
            }
        } else {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO economyexpand_wallets(player_uuid,currency,balance) VALUES(?,?,?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, currency.toLowerCase());
                insert.setString(3, value.toPlainString());
                insert.executeUpdate();
            }
        }
    }

    public synchronized void close() {
        try { connection.close(); } catch (SQLException ignored) { }
    }
}
