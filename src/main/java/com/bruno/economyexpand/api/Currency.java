package com.bruno.economyexpand.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Currency {
    private final String id, name, symbol, singular, plural;
    private final int decimals;
    private final BigDecimal startingBalance, maxBalance;
    private final boolean enabled;
    public Currency(String id, String name, String symbol, String singular, String plural, int decimals,
                   BigDecimal startingBalance, BigDecimal maxBalance, boolean enabled) {
        this.id=id; this.name=name; this.symbol=symbol; this.singular=singular; this.plural=plural;
        this.decimals=decimals; this.startingBalance=startingBalance; this.maxBalance=maxBalance; this.enabled=enabled;
    }
    public String getId(){return id;} public String getName(){return name;} public String getSymbol(){return symbol;}
    public String getSingular(){return singular;} public String getPlural(){return plural;}
    public int getDecimals(){return decimals;} public BigDecimal getStartingBalance(){return startingBalance;}
    public BigDecimal getMaxBalance(){return maxBalance;} public boolean isEnabled(){return enabled;}
    public BigDecimal normalize(BigDecimal value){return value.setScale(decimals, RoundingMode.DOWN);}
    public String format(BigDecimal value){return normalize(value).toPlainString();}
}