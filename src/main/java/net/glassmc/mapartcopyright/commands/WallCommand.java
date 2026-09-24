package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.artwork.ArtworkSize;
import net.glassmc.mapartcopyright.service.ArtworkService;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import java.sql.SQLException;
import java.util.Arrays;

public final class WallCommand implements SubCommand {
    @Override public String getName() { return "wall"; }

    @Override public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Messages.send(sender, "only-players", "§cOnly players can use this command."); return; }
        if (!player.hasPermission("mapart.use") || !player.hasPermission("mapart.wall")) {
            Messages.send(player, "no-permission", "§cYou don't have permission to do that."); return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
            try {
                ArtworkSize size = ArtworkSize.parse(args[2]);
                if (!(player.getTargetEntity(6, false) instanceof ItemFrame frame)) {
                    player.sendMessage("§cLook directly at the top-left frame of the artwork, within six blocks."); return;
                }
                String title = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                ArtworkService.create(player, frame, size, title);
            } catch (IllegalArgumentException ex) { player.sendMessage("§c" + ex.getMessage()); }
            return;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("info")) {
            try {
                var member = ArtworkService.find(player.getInventory().getItemInMainHand());
                if (member == null && player.getTargetEntity(6, false) instanceof ItemFrame frame) member = ArtworkService.find(frame.getItem());
                if (member == null) { player.sendMessage("§cHold an artwork tile or look at its frame."); return; }
                var artwork = member.artwork();
                player.sendMessage("§6Artwork: §f" + PlainTextComponentSerializer.plainText().serialize(MapMetadata.MINI.deserialize(artwork.name())));
                player.sendMessage("§7UUID: §f" + artwork.id());
                player.sendMessage("§7Owner: §f" + artwork.owner());
                player.sendMessage("§7Layout: §f" + artwork.size() + " §7| Tile: §f" + (member.tile().x() + 1) + "," + (member.tile().y() + 1));
                player.sendMessage("§7Copy lock: §f" + artwork.locked() + " §7| Frame protection: §f" + artwork.frameLocked());
                player.sendMessage("§7Hold any tile and use the normal /mapart commands to update the whole artwork.");
            } catch (SQLException ex) { player.sendMessage("§cArtwork data is unavailable. Restore database access before trying again."); }
            return;
        }
        player.sendMessage("§e/mapart wall create <2x1|2x2|2x3|3x3> [title]");
        player.sendMessage("§7Look at the top-left filled frame. Dimensions are width x height.");
        player.sendMessage("§7Registration locks all tiles; the normal lock fee applies per newly locked tile.");
        player.sendMessage("§e/mapart wall info §7- Inspect an artwork tile or frame.");
    }
}
