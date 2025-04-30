package net.glassmc.mapartcopyright.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHandler {

    private static Economy economy;

    public static boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider != null) {
            economy = provider.getProvider();
        }

        return economy != null;
    }

    public static Economy get() {
        return economy;
    }
}
