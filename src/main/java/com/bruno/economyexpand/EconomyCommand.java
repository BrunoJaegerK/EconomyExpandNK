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
        if(args[0].equalsIgnoreCase("reload")){
            if(!sender.hasPermission("economyexpand.admin")){sender.sendMessage(plugin.message("no-permission"));return true;}
            plugin.reloadFiles();sender.sendMessage(plugin.message("reloaded"));return true;
        }
        if(args[0].equalsIgnoreCase("balance")||args[0].equalsIgnoreCase("bal")){
            Player target;String currency=plugin.defaultCurrency();
            if(sender instanceof Player){target=(Player)sender;if(args.length>1)currency=args[1];}
            else {if(args.length<2){sender.sendMessage(plugin.message("usage"));return true;}target=plugin.getServer().getPlayer(args[1]);if(target==null){sender.sendMessage(plugin.message("error"));return true;}if(args.length>2)currency=args[2];}
            if(target!=sender&&!sender.hasPermission("economyexpand.balance.others")){sender.sendMessage(plugin.message("no-permission"));return true;}
            Currency c=plugin.currency(currency);if(c==null||!c.isEnabled()){sender.sendMessage(plugin.render("unknown-currency",Collections.singletonMap("currency",currency)));return true;}
            BigDecimal balance=plugin.getEconomy().getBalance(target.getUniqueId(),currency);
            Map<String,String> vals=new HashMap<>();vals.put("player",target.getName());vals.put("symbol",c.getSymbol());vals.put("balance",c.format(balance));vals.put("currency",c.getName());
            sender.sendMessage(plugin.render(target==sender?"balance-self":"balance",vals));return true;
        }
        sender.sendMessage(plugin.message("usage"));return true;
    }
}