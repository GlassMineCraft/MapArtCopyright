package net.glassmc.mapartcopyright.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

/**
 * Utility for sanitizing user input like map names and creator credits.
 */
public class StringSanitizer {

    /**
     * Cleans a string by:
     * - Trimming whitespace
     * - Removing all control characters
     * - Limiting maximum length
     *
     * @param input     Raw input from player
     * @param maxLength Maximum length allowed
     * @return Sanitized string
     */
    public static String clean(String input, int maxLength) {
        if (input == null) return "";

        // Remove non-printable and illegal characters except Minecraft color codes (§)
        String stripped = input.replaceAll("[^\\p{Print}&&[^§]]", "").trim();

        if (stripped.length() > maxLength) {
            stripped = stripped.substring(0, maxLength);
        }

        return stripped;
    }

    /**
     * Parses a player-provided string into an Adventure Component supporting
     * MiniMessage RGB colors (e.g. {@code <#FF66CC>Text</#FF66CC>}) and legacy
     * {@code &}-style codes. The visible text length is limited after parsing.
     *
     * @param input     Raw input from player
     * @param maxLength Maximum number of visible characters allowed
     * @return Parsed Component
     * @throws IllegalArgumentException if parsing fails or the text is too long
     */
    public static Component parseComponent(String input, int maxLength) {
        if (input == null) return Component.empty();

        String trimmed = input.trim();

        Component component;
        try {
            if (trimmed.contains("<") || trimmed.contains(">")) {
                // Strict MiniMessage parsing allowing only color and decoration tags
                MiniMessage mm = MiniMessage.builder()
                        .strict(true)
                        .tags(TagResolver.builder()
                                .resolver(StandardTags.color())
                                .resolver(StandardTags.decorations())
                                .build())
                        .build();
                component = mm.deserialize(trimmed);
            } else {
                // Fallback to legacy & codes (supports hex with &#RRGGBB)
                component = LegacyComponentSerializer.legacyAmpersand().deserialize(trimmed);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid color codes. Use <#RRGGBB>Text</#RRGGBB>.");
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(component);
        if (plain.length() > maxLength) {
            throw new IllegalArgumentException("Name too long (max " + maxLength + " characters).");
        }

        return component;
    }
}
