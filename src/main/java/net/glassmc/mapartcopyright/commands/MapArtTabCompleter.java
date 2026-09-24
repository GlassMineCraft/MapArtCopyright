package net.glassmc.mapartcopyright.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapArtTabCompleter implements TabCompleter {

        private static final List<String> SUBCOMMANDS = Arrays.asList(
                    "lock", "unlock", "name", "credit", "menu", "wall", "audit", "info", "verify", "help", "export"
                );
 

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
	    if (args.length == 1) {
	        return SUBCOMMANDS.stream()
	                .filter(sub -> {
	                    if (sub.equals("audit") && !sender.hasPermission("mapart.audit")) return false;
                        if (sub.equals("wall") && !sender.hasPermission("mapart.wall")) return false;
	                    return sub.startsWith(args[0].toLowerCase());
	                })
	                .collect(Collectors.toList());
	    }

        if (args[0].equalsIgnoreCase("wall") && sender.hasPermission("mapart.wall")) {
            if (args.length == 2) return List.of("create", "info").stream()
                    .filter(value -> value.startsWith(args[1].toLowerCase(java.util.Locale.ROOT))).toList();
            if (args.length == 3 && args[1].equalsIgnoreCase("create")) return net.glassmc.mapartcopyright.artwork.ArtworkSize.PRESETS.stream()
                    .filter(value -> value.startsWith(args[2].toLowerCase(java.util.Locale.ROOT))).toList();
        }
	    if (args.length == 2) {
	        if (args[0].equalsIgnoreCase("credit") || args[0].equalsIgnoreCase("name")) {
	            if (sender instanceof Player player) {
	                return Collections.singletonList(player.getName());
	            }
	        }

	        if (args[0].equalsIgnoreCase("verify") && sender.hasPermission("mapart.verify.others")) {
	            return Bukkit.getOnlinePlayers().stream()
	                    .map(Player::getName)
	                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
	                    .collect(Collectors.toList());
	        }
	    }

	    return Collections.emptyList();
	}

}
