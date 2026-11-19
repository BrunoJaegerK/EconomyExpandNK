package com.bruno.economyexpand.storage;
import java.math.BigDecimal; import java.sql.*; import java.util.UUID;
public class SqlWalletStorage implements WalletStorage {
 private final Connection connection;
 public SqlWalletStorage(String url,String user,String password)throws SQLException { connection=(user==null||user.trim().isEmpty())?DriverManager.getConnection(url):DriverManager.getConnection(url,user,password==null?"":password); try(Statement s=connection.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS economyexpand_wallets (player_uuid VARCHAR(36) NOT NULL, currency VARCHAR(64) NOT NULL, balance VARCHAR(64) NOT NULL, PRIMARY KEY(player_uuid,currency))");}}
 public synchronized BigDecimal read(UUID uuid,String currency,BigDecimal fallback){try(PreparedStatement p=connection.prepareStatement("SELECT balance FROM economyexpand_wallets WHERE player_uuid=? AND currency=?")){p.setString(1,uuid.toString());p.setString(2,currency.toLowerCase());try(ResultSet r=p.executeQuery()){return r.next()?new BigDecimal(r.getString(1)):fallback;}}catch(SQLException e){throw new IllegalStateException("Wallet read failed",e);}}
 public synchronized void write(UUID uuid,String currency,BigDecimal value){try{connection.setAutoCommit(false);try(PreparedStatement u=connection.prepareStatement("UPDATE economyexpand_wallets SET balance=? WHERE player_uuid=? AND currency=?")){u.setString(1,value.toPlainString());u.setString(2,uuid.toString());u.setString(3,currency.toLowerCase());if(u.executeUpdate()==0)try(PreparedStatement i=connection.prepareStatement("INSERT INTO economyexpand_wallets(player_uuid,currency,balance) VALUES(?,?,?)")){i.setString(1,uuid.toString());i.setString(2,currency.toLowerCase());i.setString(3,value.toPlainString());i.executeUpdate();}}connection.commit();}catch(SQLException e){try{connection.rollback();}catch(SQLException ignored){}throw new IllegalStateException("Wallet write failed",e);}finally{try{connection.setAutoCommit(true);}catch(SQLException ignored){}}}
 public synchronized boolean transfer(UUID from,UUID to,String currency,BigDecimal amount,BigDecimal fromDefault,BigDecimal toDefault){
  try{connection.setAutoCommit(false);BigDecimal source=read(from,currency,fromDefault),target=read(to,currency,toDefault);if(source.compareTo(amount)<0){connection.rollback();return false;}upsert(from,currency,source.subtract(amount));upsert(to,currency,target.add(amount));connection.commit();return true;}catch(SQLException|RuntimeException e){try{connection.rollback();}catch(SQLException ignored){}return false;}finally{try{connection.setAutoCommit(true);}catch(SQLException ignored){}}
 }
 private void upsert(UUID uuid,String currency,BigDecimal value)throws SQLException{
  try(PreparedStatement u=connection.prepareStatement("UPDATE economyexpand_wallets SET balance=? WHERE player_uuid=? AND currency=?")){
   u.setString(1,value.toPlainString());u.setString(2,uuid.toString());u.setString(3,currency.toLowerCase());
   if(u.executeUpdate()==0)try(PreparedStatement ins=connection.prepareStatement("INSERT INTO economyexpand_wallets(player_uuid,currency,balance) VALUES(?,?,?)")){
    ins.setString(1,uuid.toString());ins.setString(2,currency.toLowerCase());ins.setString(3,value.toPlainString());ins.executeUpdate();
   }
  }
 }
 public synchronized void close(){try{connection.close();}catch(SQLException ignored){}}
}