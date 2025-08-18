package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.util.CreditUtil;
import net.glassmc.mapartcopyright.util.StringSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class CreditCommand implements SubCommand {

    @Override
    public String getName() {
        return "credit";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can set credit.");
            return;
        }

        if (!player.hasPermission("mapart.credit")) {
            player.sendMessage("§cYou don’t have permission to do this.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /mapart credit <name>");
            return;
        }

        String raw = String.join(" ", args).substring(args[0].length()).trim();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getItemMeta() == null || !(item.getItemMeta() instanceof MapMeta)) {
            player.sendMessage("§cHold a filled map to credit it.");
            return;
        }

        Component creditComponent;
        try {
            creditComponent = StringSanitizer.parseComponent(raw, 16);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
            return;
        }

        String credit = LegacyComponentSerializer.legacySection().serialize(creditComponent);
        boolean success = CreditUtil.setCredit(item, credit, player);
        if (success) {
            player.sendMessage(
                Component.text("Map credited to: ", NamedTextColor.GREEN)
                    .append(creditComponent)
            );
            player.getInventory().setItemInMainHand(item);
        }
    }
}