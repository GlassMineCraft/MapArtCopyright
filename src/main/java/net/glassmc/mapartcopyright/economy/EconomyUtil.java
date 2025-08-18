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
            if (notify) player.sendMessage("§cTransaction failed: economy system not available.");
            return false;
        }

        if (eco.has(player, amount)) {
            eco.withdrawPlayer(player, amount);
            if (notify) player.sendMessage("§aTransaction successful: charged §e$" + amount + "§a.");
            return true;
        } else {
            if (notify) {
                double balance = eco.getBalance(player);
                double needed = amount - balance;
                player.sendMessage("§cTransaction failed: insufficient funds. You need §e$" + needed + "§c more.");
            }
            return false;
        }
    }

    public static double getBalance(Player player) {
        Economy eco = EconomyHandler.get();
        return eco != null ? eco.getBalance(player) : 0.0;
    }
}
