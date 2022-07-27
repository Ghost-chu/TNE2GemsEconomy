package com.ghostchu.tne2gemseconomy;

import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.account.AccountManager;
import me.xanium.gemseconomy.api.GemsEconomyAPI;
import me.xanium.gemseconomy.currency.Currency;
import net.tnemc.core.TNE;
import net.tnemc.core.common.account.TNEAccount;
import net.tnemc.core.common.currency.TNECurrency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TNE2GemsEconomy extends JavaPlugin {
    private GemsEconomy gemsEconomy;
    private AccountManager gemsAccountManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        gemsEconomy = GemsEconomy.getInstance();
        gemsAccountManager = gemsEconomy.getAccountManager();
        Set<Map.Entry<UUID, TNEAccount>> accounts = TNE.manager().getAccounts().entrySet();
        for (Map.Entry<UUID, TNEAccount> tneAccount : accounts) {
            TNEAccount account = tneAccount.getValue();
            Account gemsAccount = pickGemsEconomyAccount(tneAccount.getKey());
            getLogger().info("Converting account " + tneAccount.getKey() + " to gems economy account");
            copyAccount(gemsAccount, account);
        }
        getLogger().info("Converted "+accounts.size()+" account(s).");
    }

    private void copyAccount(Account gemsAccount, TNEAccount account){
        for (TNECurrency currency : TNE.manager().currencyManager().getCurrencies()) {
            BigDecimal decimal = account.getHoldings(currency);
            if(decimal == null) continue;
            gemsAccount.setBalance(convertCurrency(currency), decimal.doubleValue());
        }
    }

    private Currency convertCurrency(TNECurrency tneCurrency){
        try {
            Currency gemsCurrency = gemsEconomy.getCurrencyManager().getCurrency(getSingle(tneCurrency));
            if(gemsCurrency != null) return gemsCurrency;
            getLogger().info("Found an non-exists currency: "+getSingle(tneCurrency) +" converting...");
            gemsEconomy.getCurrencyManager().createNewCurrency(getSingle(tneCurrency),getPlural(tneCurrency));
            gemsCurrency = gemsEconomy.getCurrencyManager().getCurrency(getSingle(tneCurrency));
            if(gemsCurrency == null) throw new IllegalStateException("Failed to create currency");
            gemsCurrency.setSymbol(getSymbol(tneCurrency));
            return gemsCurrency;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Account pickGemsEconomyAccount(UUID user){
        Account gemsAccount = gemsAccountManager.getAccount(user);
        if (gemsAccount == null) {
            getLogger().info("Creating account for " + user);
            gemsAccountManager.createAccount(user, Bukkit.getOfflinePlayer(user).getName());
        }
        gemsAccount =gemsAccountManager.getAccount(user);
        return gemsAccount;
    }

    private String getPlural(TNECurrency currency) throws NoSuchFieldException, IllegalAccessException {
       Field field = currency.getClass().getDeclaredField("plural");
       field.setAccessible(true);
         return (String) field.get(currency);
    }

    private String getSymbol(TNECurrency currency) throws NoSuchFieldException, IllegalAccessException {
        Field field = currency.getClass().getDeclaredField("symbol");
        field.setAccessible(true);
        return (String) field.get(currency);
    }
    private String getSingle(TNECurrency currency) throws NoSuchFieldException, IllegalAccessException {
        Field field = currency.getClass().getDeclaredField("single");
        field.setAccessible(true);
        return (String) field.get(currency);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
