package net.glassmc.mapartcopyright.economy;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class EconomyUtil {
    public record Receipt(Economy provider, double amount) {}
    private EconomyUtil() {}

    public static boolean isEnabled() { return MapArtCopyright.getInstance().getConfig().getBoolean("economy.enabled", true); }
    public static double getCost(String action) { return MapArtCopyright.getInstance().getConfig().getDouble("economy." + action + "-cost", 0); }

    public static Receipt withdraw(Player player, String action) {
        return withdraw(player, action, 1);
    }

    /** Charge one provider transaction for a bounded number of changed tiles. */
    public static Receipt withdraw(Player player, String action, int units) {
        if (units < 0 || units > 9) throw new IllegalArgumentException("Invalid number of tiles to charge");
        double amount = isEnabled() && !player.hasPermission("mapart.free") ? getCost(action) * units : 0;
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalStateException("The configured fee is invalid. Contact an administrator.");
        if (amount == 0) return new Receipt(null, 0);
        Economy economy = EconomyHandler.get();
        if (economy == null) throw new IllegalStateException("The economy provider is unavailable; nothing was changed.");
        var response = economy.withdrawPlayer(player, amount);
        if (response == null || !response.transactionSuccess()) throw new IllegalStateException("Payment failed; nothing was changed.");
        return new Receipt(economy, amount);
    }

    public static void refund(Player player, Receipt receipt, String operation) {
        if (receipt == null || receipt.amount() == 0) return;
        try {
            var response = receipt.provider().depositPlayer(player, receipt.amount());
            if (response != null && response.transactionSuccess()) return;
        } catch (RuntimeException ignored) { /* Persist a manual reconciliation record below. */ }
        String entry = Instant.now() + " player=" + player.getUniqueId() + " amount=" + receipt.amount() + " operation=" + operation;
        MapArtCopyright.getInstance().getLogger().severe("REFUND REQUIRED: " + entry);
        try {
            Files.writeString(MapArtCopyright.getInstance().getDataFolder().toPath().resolve("refunds-pending.log"),
                    entry + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            MapArtCopyright.getInstance().getLogger().severe("Could not write refund record: " + ex.getMessage());
        }
        player.sendMessage("§cThe operation failed and your refund needs administrator attention.");
    }

    public static double getBalance(Player player) {
        Economy economy = EconomyHandler.get();
        return economy == null ? 0 : economy.getBalance(player);
    }

    /** Compatibility helper: checks the actual provider result. */
    public static boolean charge(Player player, double amount, boolean notify) {
        Economy economy = EconomyHandler.get();
        if (!Double.isFinite(amount) || amount < 0 || economy == null) return false;
        var result = economy.withdrawPlayer(player, amount);
        boolean success = result != null && result.transactionSuccess();
        if (notify) player.sendMessage(success ? "§aPayment completed." : "§cPayment failed.");
        return success;
    }

    public static boolean charge(Player player, double amount) { return charge(player, amount, false); }
}
