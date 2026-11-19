package com.bruno.economyexpand;

import cn.nukkit.Player;
import com.bruno.economyexpand.api.Currency;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Registers visitor-sensitive placeholders in CreeperFace's Nukkit PlaceholderAPI.
 * Each balance placeholder is evaluated for the player viewing the text.
 */
public final class EconomyExpandPlaceholders {
    private final EconomyExpandNK plugin;

    public EconomyExpandPlaceholders(EconomyExpandNK plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderAPI api = PlaceholderAPI.getInstance();
        if (api == null) {
            plugin.getLogger().warning("PlaceholderAPI está instalado, pero su API todavía no está disponible.");
            return;
        }

        api.builder("economyexpand_balance", String.class)
                .visitorLoader(entry -> balance(entry.getPlayer(), plugin.defaultCurrency()))
                .build();

        api.builder("economyexpand_total_currencies", String.class)
                .loader(entry -> String.valueOf(plugin.getEnabledCurrencyCount()))
                .build();

        for (Currency currency : plugin.getEconomy().getCurrencies()) {
            if (!currency.isEnabled()) {
                continue;
            }

            final String id = currency.getId().toLowerCase(Locale.ROOT);
            final String suffix = id.replaceAll("[^a-z0-9_]", "_");

            api.builder("economyexpand_balance_" + suffix, String.class)
                    .visitorLoader(entry -> balance(entry.getPlayer(), id))
                    .build();

            api.builder("economyexpand_balance_formatted_" + suffix, String.class)
                    .visitorLoader(entry -> formattedBalance(entry.getPlayer(), id))
                    .build();

            api.builder("economyexpand_symbol_" + suffix, String.class)
                    .loader(entry -> currentCurrencyValue(id, 0))
                    .build();

            api.builder("economyexpand_name_" + suffix, String.class)
                    .loader(entry -> currentCurrencyValue(id, 1))
                    .build();
        }

        plugin.getLogger().info("Placeholders de PlaceholderAPI registrados para " +
                plugin.getEnabledCurrencyCount() + " monedas.");
    }

    private String balance(Player player, String currencyId) {
        if (player == null || plugin.getEconomy() == null) {
            return "0";
        }

        Currency currency = plugin.getEconomy().getCurrency(currencyId);
        if (currency == null || !currency.isEnabled()) {
            return "0";
        }

        try {
            BigDecimal amount = plugin.getEconomy().getBalance(player.getUniqueId(), currencyId);
            return currency.format(amount);
        } catch (RuntimeException exception) {
            plugin.getLogger().error("No se pudo obtener el saldo de " + player.getName() +
                    " para la moneda " + currencyId, exception);
            return "0";
        }
    }

    private String formattedBalance(Player player, String currencyId) {
        Currency currency = plugin.getEconomy() == null ? null : plugin.getEconomy().getCurrency(currencyId);
        if (currency == null || !currency.isEnabled()) {
            return "0";
        }
        return currency.getSymbol() + balance(player, currencyId);
    }

    private String currentCurrencyValue(String currencyId, int field) {
        Currency currency = plugin.getEconomy() == null ? null : plugin.getEconomy().getCurrency(currencyId);
        if (currency == null || !currency.isEnabled()) {
            return "";
        }
        return field == 0 ? currency.getSymbol() : currency.getName();
    }
}
