package com.bruno.economyexpand.storage;
import com.mongodb.client.MongoClient;import com.mongodb.client.MongoClients;import com.mongodb.client.MongoCollection;import com.mongodb.client.MongoDatabase;import com.mongodb.client.model.ReplaceOptions;import org.bson.Document;import java.math.BigDecimal;import java.util.UUID;
public class MongoWalletStorage implements WalletStorage {
 private final MongoClient client;private final MongoCollection<Document> wallets;
 public MongoWalletStorage(String uri,String database,String collection){client=MongoClients.create(uri);MongoDatabase db=client.getDatabase(database);wallets=db.getCollection(collection);}
 private String key(UUID uuid,String currency){return uuid.toString()+"|"+currency.toLowerCase();}
 public synchronized BigDecimal read(UUID uuid,String currency,BigDecimal fallback){Document d=wallets.find(new Document("_id",key(uuid,currency))).first();if(d==null)return fallback;try{return new BigDecimal(d.getString("balance"));}catch(Exception e){return fallback;}}
 public synchronized void write(UUID uuid,String currency,BigDecimal value){Document d=new Document("_id",key(uuid,currency)).append("uuid",uuid.toString()).append("currency",currency.toLowerCase()).append("balance",value.toPlainString());wallets.replaceOne(new Document("_id",d.getString("_id")),d,new ReplaceOptions().upsert(true));}
 public void close(){client.close();}
}