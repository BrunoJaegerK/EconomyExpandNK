package com.bruno.economyexpand;

import cn.nukkit.plugin.PluginBase;
import com.bruno.economyexpand.api.Currency;
import com.bruno.economyexpand.api.SecondaryEconomy;
import com.bruno.economyexpand.storage.WalletStorage;
import com.bruno.economyexpand.storage.YamlWalletStorage;
import com.bruno.economyexpand.storage.SqlWalletStorage;
import com.bruno.economyexpand.storage.MongoWalletStorage;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class EconomyExpandNK extends PluginBase {
    private static EconomyExpandNK instance;
    private final Map<String,Currency> currencies=new LinkedHashMap<>();
    private WalletStorage storage;
    private SecondaryEconomy economy;
    private cn.nukkit.utils.Config messages;
    public static EconomyExpandNK getInstance(){return instance;}
    public SecondaryEconomy getEconomy(){return economy;}
    public Currency currency(String id){return id==null?null:currencies.get(id.toLowerCase(Locale.ROOT));}
    public String defaultCurrency(){String preferred=getConfig().getString("default-currency","crystals");Currency c=currency(preferred);if(c!=null&&c.isEnabled())return preferred;for(Currency item:currencies.values())if(item.isEnabled())return item.getId();return preferred;}
    @Override public void onEnable(){
        instance=this;saveDefaultConfig();saveResource("currencies.yml",false);saveResource("messages.yml",false);reloadFiles();
        try {storage=createStorage();} catch(Exception ex){getLogger().error("No se pudo inicializar el almacenamiento",ex);getServer().getPluginManager().disablePlugin(this);return;}
        economy=new EconomyImpl();getServer().getCommandMap().register("economyexpand",new EconomyCommand(this));
        getLogger().info("EconomyExpandNK habilitado ("+currencies.size()+" monedas, storage="+getConfig().getSection("storage").getString("type","yaml")+")");
    }
    private WalletStorage createStorage() throws Exception {
        cn.nukkit.utils.ConfigSection storageCfg=getConfig().getSection("storage");
        String type=storageCfg.getString("type","yaml").toLowerCase(Locale.ROOT);
        cn.nukkit.utils.ConfigSection sqlCfg=storageCfg.getSection("sql");
        cn.nukkit.utils.ConfigSection mysqlCfg=sqlCfg.getSection("mysql");
        cn.nukkit.utils.ConfigSection mongoCfg=storageCfg.getSection("mongodb");
        if("yaml".equals(type))return new YamlWalletStorage(getDataFolder()+"/wallets.yml");
        if("sqlite".equals(type)){
            String file=sqlCfg.getString("sqlite-file","wallets.db");
            return new SqlWalletStorage("jdbc:sqlite:"+getDataFolder()+"/"+file,null,null);
        }
        if("h2".equals(type)){
            String file=sqlCfg.getString("h2-file","wallets-h2");
            return new SqlWalletStorage("jdbc:h2:file:"+getDataFolder()+"/"+file,null,null);
        }
        if("mysql".equals(type)){
            String host=mysqlCfg.getString("host","127.0.0.1");
            int port=mysqlCfg.getInt("port",3306);
            String db=mysqlCfg.getString("database","economyexpand");
            String user=mysqlCfg.getString("username","root");
            String pass=mysqlCfg.getString("password","");
            boolean ssl=mysqlCfg.getBoolean("use-ssl",false);
            String url="jdbc:mysql://"+host+":"+port+"/"+db+"?useSSL="+ssl+"&serverTimezone=UTC";
            return new SqlWalletStorage(url,user,pass);
        }
        if("mongodb".equals(type)){
            return new MongoWalletStorage(mongoCfg.getString("uri","mongodb://127.0.0.1:27017"),
                mongoCfg.getString("database","economyexpand"),
                mongoCfg.getString("collection","wallets"));
        }
        throw new IllegalArgumentException("Unknown storage provider: "+type);
    }
    public void reloadFiles(){
        reloadConfig();currencies.clear();
        cn.nukkit.utils.Config cfg=new cn.nukkit.utils.Config(getDataFolder()+"/currencies.yml",cn.nukkit.utils.Config.YAML);
        Map<?,?> root=cfg.get("currencies") instanceof Map?(Map<?,?>)cfg.get("currencies"):Collections.emptyMap();
        for(Map.Entry<?,?> entry:root.entrySet()){
            String id=String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);if(!(entry.getValue() instanceof Map))continue;
            Map<?,?> m=(Map<?,?>)entry.getValue();int decimals=Math.max(0,Math.min(8,integer(m.get("decimals"),0)));
            Currency c=new Currency(id,str(m.get("name"),id),str(m.get("symbol"),""),str(m.get("singular"),id),str(m.get("plural"),id),
                decimals,decimal(m.get("starting-balance"),BigDecimal.ZERO),decimal(m.get("max-balance"),new BigDecimal("999999999")),bool(m.get("enabled"),true));
            currencies.put(id,c);
        }
        messages=new cn.nukkit.utils.Config(getDataFolder()+"/messages.yml",cn.nukkit.utils.Config.YAML);
    }
    public String message(String key){return messages.getString(key,"§cMissing message: "+key);}
    public String render(String key,Map<String,String> values){String result=message(key);for(Map.Entry<String,String> e:values.entrySet())result=result.replace("{"+e.getKey()+"}",e.getValue());return message("prefix")+result;}
    private static String str(Object o,String d){return o==null?d:String.valueOf(o);}
    private static int integer(Object o,int d){try{return Integer.parseInt(String.valueOf(o));}catch(Exception e){return d;}}
    private static boolean bool(Object o,boolean d){return o==null?d:Boolean.parseBoolean(String.valueOf(o));}
    private static BigDecimal decimal(Object o,BigDecimal d){try{return new BigDecimal(String.valueOf(o));}catch(Exception e){return d;}}
    @Override public void onDisable(){if(storage!=null)storage.close();}
    private class EconomyImpl implements SecondaryEconomy {
        @Override public Currency getCurrency(String id){return currency(id);}
        @Override public Collection<Currency> getCurrencies(){return Collections.unmodifiableCollection(currencies.values());}
        private Currency require(String id){Currency c=currency(id);if(c==null||!c.isEnabled())throw new IllegalArgumentException("Currency not found or disabled: "+id);return c;}
        @Override public synchronized BigDecimal getBalance(UUID p,String id){Currency c=require(id);return storage.read(p,c.getId(),c.getStartingBalance()).setScale(c.getDecimals(),java.math.RoundingMode.DOWN);}
        @Override public synchronized boolean deposit(UUID p,String id,BigDecimal a){if(!valid(a))return false;Currency c=require(id);BigDecimal amount=c.normalize(a);if(amount.signum()<=0)return false;BigDecimal n=getBalance(p,id).add(amount);if(n.compareTo(c.getMaxBalance())>0)return false;storage.write(p,id,n);return true;}
        @Override public synchronized boolean withdraw(UUID p,String id,BigDecimal a){if(!valid(a))return false;Currency c=require(id);BigDecimal amount=c.normalize(a);if(amount.signum()<=0)return false;BigDecimal old=getBalance(p,id),n=old.subtract(amount);if(n.signum()<0)return false;storage.write(p,id,n);return true;}
        @Override public synchronized boolean setBalance(UUID p,String id,BigDecimal a){if(a==null||a.signum()<0)return false;Currency c=require(id);BigDecimal n=c.normalize(a);if(n.compareTo(c.getMaxBalance())>0)return false;storage.write(p,id,n);return true;}
        @Override public synchronized boolean transfer(UUID from,UUID to,String id,BigDecimal a){if(from.equals(to)||!valid(a))return false;Currency c=require(id);BigDecimal amount=c.normalize(a),b1=getBalance(from,id),b2=getBalance(to,id);if(amount.signum()<=0||b1.compareTo(amount)<0||b2.add(amount).compareTo(c.getMaxBalance())>0)return false;return storage.transfer(from,to,id,amount,c.getStartingBalance(),c.getStartingBalance());}
        private boolean valid(BigDecimal a){return a!=null&&a.signum()>0;}
    }
}