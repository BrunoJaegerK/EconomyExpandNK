package com.bruno.economyexpand;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.bruno.economyexpand.api.Currency;
import java.math.BigDecimal;
import java.util.*;

public class EconomyCommand extends Command {
    private final EconomyExpandNK plugin;

    public EconomyCommand(EconomyExpandNK plugin) {
        super("ecx", "EconomyExpandNK economy commands", "/ecx help", new String[]{"economyexpand", "ec"});
        this.plugin = plugin;
        setPermission("economyexpand.command");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("economyexpand.command")) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(plugin.message("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!sender.hasPermission("economyexpand.admin")) {
                sender.sendMessage(plugin.message("no-permission"));
                return true;
            }
            plugin.reloadFiles();
            sender.sendMessage(plugin.message("reloaded"));
            return true;
        }
        if (sub.equals("currencies") || sub.equals("currency") || sub.equals("monedas")) {
            StringBuilder list = new StringBuilder(plugin.message("currencies-header"));
            for (Currency c : plugin.getEconomy().getCurrencies()) {
                if (c.isEnabled()) list.append("\n§f").append(c.getId()).append(" §7- §a").append(c.getSymbol()).append(" ").append(c.getName());
            }
            sender.sendMessage(list.toString());
            return true;
        }
        if (sub.equals("balance") || sub.equals("bal") || sub.equals("money")) {
            balance(sender, args);
            return true;
        }
        if (sub.equals("pay")) {
            transaction(sender, args, "pay");
            return true;
        }
        if (sub.equals("give") || sub.equals("add")) {
            transaction(sender, args, "give");
            return true;
        }
        if (sub.equals("take") || sub.equals("remove") || sub.equals("reduce") || sub.equals("withdraw")) {
            transaction(sender, args, "take");
            return true;
        }
        if (sub.equals("set")) {
            transaction(sender, args, "set");
            return true;
        }
        sender.sendMessage(plugin.message("usage"));
        return true;
    }

    private void balance(CommandSender sender, String[] args) {
        Player target = null;
        String id = plugin.defaultCurrency();

        if (sender instanceof Player) {
            Player self = (Player) sender;
            if (args.length == 1) {
                target = self;
            } else if (plugin.currency(args[1]) != null) {
                target = self;
                id = args[1];
            } else {
                target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.message("player-not-found"));
                    return;
                }
                if (!sender.hasPermission("economyexpand.balance.others")) {
                    sender.sendMessage(plugin.message("no-permission"));
                    return;
                }
                if (args.length > 2) id = args[2];
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage(plugin.message("usage"));
                return;
            }
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.message("player-not-found"));
                return;
            }
            if (args.length > 2) id = args[2];
        }

        Currency c = plugin.currency(id);
        if (c == null || !c.isEnabled()) {
            sender.sendMessage(plugin.render("unknown-currency", Collections.singletonMap("currency", id)));
            return;
        }
        BigDecimal balance = plugin.getEconomy().getBalance(target.getUniqueId(), id);
        Map<String, String> v = new HashMap<>();
        v.put("player", target.getName());
        v.put("symbol", c.getSymbol());
        v.put("balance", c.format(balance));
        v.put("currency", c.getName());
        sender.sendMessage(plugin.render(target == sender ? "balance-self" : "balance", v));
    }

    private void transaction(CommandSender sender, String[] args, String action) {
        boolean pay = action.equals("pay");
        Player source = sender instanceof Player ? (Player) sender : null;
        if (pay && source == null) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }
        if (!pay && !sender.hasPermission("economyexpand.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.message("usage"));
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.message("player-not-found"));
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (Exception e) {
            sender.sendMessage(plugin.message("invalid-amount"));
            return;
        }
        if (amount.signum() <= 0) {
            sender.sendMessage(plugin.message("invalid-amount"));
            return;
        }

        String id = args.length > 3 ? args[3] : plugin.defaultCurrency();
        Currency c = plugin.currency(id);
        if (c == null || !c.isEnabled()) {
            sender.sendMessage(plugin.render("unknown-currency", Collections.singletonMap("currency", id)));
            return;
        }

        boolean success;
        if (pay) success = plugin.getEconomy().transfer(source.getUniqueId(), target.getUniqueId(), id, amount);
        else if (action.equals("give")) success = plugin.getEconomy().deposit(target.getUniqueId(), id, amount);
        else if (action.equals("take")) success = plugin.getEconomy().withdraw(target.getUniqueId(), id, amount);
        else success = plugin.getEconomy().setBalance(target.getUniqueId(), id, amount);

        if (!success) {
            sender.sendMessage(plugin.render("transaction-failed", Collections.singletonMap("currency", c.getName())));
            return;
        }

        Map<String, String> values = new HashMap<>();
        values.put("player", target.getName());
        values.put("sender", source == null ? sender.getName() : source.getName());
        values.put("symbol", c.getSymbol());
        values.put("amount", c.format(c.normalize(amount)));
        values.put("currency", c.getName());
        sender.sendMessage(plugin.render(pay ? "paid" : "transaction-success", values));
        if (pay) target.sendMessage(plugin.render("received", values));
    }
}
