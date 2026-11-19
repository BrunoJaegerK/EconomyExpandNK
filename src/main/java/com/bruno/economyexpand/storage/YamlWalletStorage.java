package com.bruno.economyexpand.storage;

import cn.nukkit.utils.Config;
import java.math.BigDecimal;
import java.util.UUID;

public class YamlWalletStorage implements WalletStorage {
    private final Config data;
    public YamlWalletStorage(String file){ data=new Config(file, Config.YAML); }
    private String key(UUID uuid,String currency){return uuid.toString()+"."+currency.toLowerCase();}
    @Override public synchronized BigDecimal read(UUID uuid,String currency,BigDecimal fallback){
        Object value=data.get(key(uuid,currency)); if(value==null)return fallback;
        try{return new BigDecimal(String.valueOf(value));}catch(Exception ignored){return fallback;}
    }
    @Override public synchronized void write(UUID uuid,String currency,BigDecimal value){data.set(key(uuid,currency),value.toPlainString());data.save();}
    @Override public void close(){data.save();}
}