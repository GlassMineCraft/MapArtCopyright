package net.glassmc.mapartcopyright.artwork;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.meta.MapMeta;
import java.util.ArrayList;
import java.util.List;

public final class WallSelection {
    private WallSelection() {}

    /** Screen-right when looking toward the front of the frame. */
    public static BlockFace rightOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case EAST -> BlockFace.NORTH;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> throw new IllegalArgumentException("Artwork layouts require a vertical wall, not floor or ceiling frames.");
        };
    }

    public static List<ItemFrame> collect(ItemFrame topLeft, ArtworkSize size) {
        if (!topLeft.isValid()) throw new IllegalArgumentException("The selected frame is no longer available.");
        BlockFace facing = topLeft.getFacing();
        var right = rightOf(facing).getDirection();
        Location origin = topLeft.getLocation();
        var nearby = topLeft.getWorld().getNearbyEntities(origin, size.width() + 1, size.height() + 1, size.width() + 1);
        List<ItemFrame> result = new ArrayList<>();
        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                Location expected = origin.clone().add(right.clone().multiply(x)).add(0, -y, 0);
                List<ItemFrame> matches = nearby.stream().filter(ItemFrame.class::isInstance).map(ItemFrame.class::cast)
                        .filter(frame -> frame.isValid() && frame.getFacing() == facing
                                && frame.getLocation().distanceSquared(expected) < 0.01).toList();
                if (matches.size() != 1) throw new IllegalArgumentException("Expected exactly one frame at column " + (x + 1) + ", row " + (y + 1) + ".");
                ItemFrame frame = matches.getFirst();
                if (!(frame.getItem().getItemMeta() instanceof MapMeta) || frame.getItem().getAmount() != 1)
                    throw new IllegalArgumentException("Every frame must contain one filled map. Check column " + (x + 1) + ", row " + (y + 1) + ".");
                result.add(frame);
            }
        }
        return List.copyOf(result);
    }
}
