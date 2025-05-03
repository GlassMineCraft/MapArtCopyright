package net.glassmc.mapartcopyright.economy;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.economy.EconomyHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class EconomyUtil {

    public static boolean isEnabled() {
        return MapArtCopyright.getInstance().getConfig().getBoolean("economy.enabled");
    }

    public static double getCost(String type) {
        return MapArtCopyright.getInstance().getConfig().getDouble("economy." + type + "-cost", 0.0);
    }

    public static boolean charge(Player player, double amount) {
        return charge(player, amount, false);
    }

    public static boolean charge(Player player, double amount, boolean notify) {
        Economy eco = EconomyHandler.get();
        if (eco == null) {
            if (notify) player.sendMessage("§cEconomy system not available.");
            return false;
        }

        if (eco.has(player, amount)) {
            eco.withdrawPlayer(player, amount);
            return true;
        } else {
            if (notify) player.sendMessage("§cInsufficient funds. You need §e$" + amount);
            return false;
        }
    }

    public static double getBalance(Player player) {
        Economy eco = EconomyHandler.get();
        return eco != null ? eco.getBalance(player) : 0.0;
    }
}
