package net.glassmc.mapartcopyright.util;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CreditUtil {
    private CreditUtil() {}
    public static String getCredit(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
    }
    public static boolean setCredit(ItemStack item, String name, Player player) { return MapArtService.credit(player, item, name); }
    public static boolean isCreator(ItemStack item, Player player) {
        String credit = getCredit(item);
        return credit != null && PlainTextComponentSerializer.plainText().serialize(MapMetadata.LEGACY.deserialize(credit)).equalsIgnoreCase(player.getName());
    }
}
