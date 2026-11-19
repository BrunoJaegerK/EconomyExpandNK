package com.bruno.economyexpand;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.bruno.economyexpand.api.Currency;
import java.math.BigDecimal;
import java.util.*;

public class EconomyCommand extends Command {
    private final EconomyExpandNK plugin;
    public EconomyCommand(EconomyExpandNK plugin){super("ecx","EconomyExpand commands","/ecx help",new String[]{"economyexpand"});this.plugin=plugin;setPermission("economyexpand.command");}
    @Override public boolean execute(CommandSender sender,String label,String[] args){
        if(!sender.hasPermission("economyexpand.command")){sender.sendMessage(plugin.message("no-permission"));return true;}
        if(args.length==0||args[0].equalsIgnoreCase("help")){sender.sendMessage(plugin.message("usage"));return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(sub.equals("reload")){if(!sender.hasPermission("economyexpand.admin")){sender.sendMessage(plugin.message("no-permission"));return true;}plugin.reloadFiles();sender.sendMessage(plugin.message("reloaded"));return true;}
        if(sub.equals("currencies")){StringBuilder list=new StringBuilder("§aMonedas disponibles: ");for(Currency c:plugin.getEconomy().getCurrencies())if(c.isEnabled())list.append("§f").append(c.getId()).append(" §7(").append(c.getSymbol()).append(c.getName()).append(") ");sender.sendMessage(list.toString());return true;}
        if(sub.equals("balance")||sub.equals("bal")){balance(sender,args);return true;}
        if(sub.equals("pay")||sub.equals("give")||sub.equals("take")||sub.equals("set")){transaction(sender,args,sub);return true;}
        sender.sendMessage(plugin.message("usage"));return true;
    }
    private void balance(CommandSender sender,String[] args){
        Player target;String id=plugin.defaultCurrency();
        if(sender instanceof Player){target=(Player)sender;if(args.length>1)id=args[1];}
        else {if(args.length<2){sender.sendMessage(plugin.message("usage"));return;}target=plugin.getServer().getPlayer(args[1]);if(target==null){sender.sendMessage(plugin.message("error"));return;}if(args.length>2)id=args[2];}
        if(target!=sender&&!sender.hasPermission("economyexpand.balance.others")){sender.sendMessage(plugin.message("no-permission"));return;}
        Currency c=plugin.currency(id);if(c==null||!c.isEnabled()){sender.sendMessage(plugin.render("unknown-currency",Collections.singletonMap("currency",id)));return;}
        BigDecimal balance=plugin.getEconomy().getBalance(target.getUniqueId(),id);
        Map<String,String> v=new HashMap<>();v.put("player",target.getName());v.put("symbol",c.getSymbol());v.put("balance",c.format(balance));v.put("currency",c.getName());
        sender.sendMessage(plugin.render(target==sender?"balance-self":"balance",v));
    }
    private void transaction(CommandSender sender,String[] args,String action){
        if(!(sender instanceof Player)){sender.sendMessage("Este comando debe ejecutarse dentro del juego.");return;}
        Player source=(Player)sender;
        boolean pay=action.equals("pay");
        if(!pay&&!sender.hasPermission("economyexpand.admin")){sender.sendMessage(plugin.message("no-permission"));return;}
        if(args.length<3){sender.sendMessage(plugin.message("usage"));return;}
        Player target=plugin.getServer().getPlayer(args[1]);if(target==null){sender.sendMessage(plugin.message("error"));return;}
        BigDecimal amount;try{amount=new BigDecimal(args[2]);}catch(Exception e){sender.sendMessage(plugin.message("error"));return;}
        String id=args.length>3?args[3]:plugin.defaultCurrency();
        Currency c=plugin.currency(id);if(c==null||!c.isEnabled()){sender.sendMessage(plugin.render("unknown-currency",Collections.singletonMap("currency",id)));return;}
        boolean success;
        if(pay)success=plugin.getEconomy().transfer(source.getUniqueId(),target.getUniqueId(),id,amount);
        else if(action.equals("give"))success=plugin.getEconomy().deposit(target.getUniqueId(),id,amount);
        else if(action.equals("take"))success=plugin.getEconomy().withdraw(target.getUniqueId(),id,amount);
        else success=plugin.getEconomy().setBalance(target.getUniqueId(),id,amount);
        if(!success){sender.sendMessage(plugin.message("error"));return;}
        Map<String,String> values=new HashMap<>();values.put("player",target.getName());values.put("symbol",c.getSymbol());values.put("amount",c.format(amount));values.put("currency",c.getName());
        sender.sendMessage(plugin.render(pay?"paid":"transaction-success",values));
        if(pay)target.sendMessage(plugin.render("received",values));
    }
}