package xyz.volcanobay.cabalist.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.system.contract.Inscription;

import java.util.ArrayList;
import java.util.List;

// Inscribed spells written in runes, a character at a time so each one wobbles on its own.
public class ClientInscriptionTooltip implements ClientTooltipComponent {
    private static final int MAX_LINE_WIDTH = 160;
    private static final int LINE_HEIGHT = 10;
    private static final int SPELL_GAP = 2;
    private static final float BOB_SPEED = 0.12f;
    private static final float BOB_SPREAD = 0.7f;
    private static final float BOB_HEIGHT = 0.8f;

    private final List<Row> rows = new ArrayList<>();
    private final int width;
    private final int height;

    public ClientInscriptionTooltip(InscriptionTooltip tooltip) {
        Font font = Minecraft.getInstance().font;
        int widest = 0;
        int y = 0;
        for (Inscription.Line line : tooltip.inscription().lines()) {
            for (String text : wrap(font, line.text(), line.tint())) {
                rows.add(new Row(text, 0xFF000000 | line.tint(), y));
                widest = Math.max(widest, font.width(rune(text, line.tint())));
                y += LINE_HEIGHT;
            }
            y += SPELL_GAP;
        }
        width = widest;
        height = Math.max(0, y - SPELL_GAP);
    }

    @Override
    public int getHeight() {
        return height + SPELL_GAP;
    }

    @Override
    public int getWidth(Font font) {
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        float time = getTime();
        int index = 0;
        for (Row row : rows) {
            int cursor = 0;
            for (int i = 0; i < row.text.length(); i++) {
                Component character = rune(String.valueOf(row.text.charAt(i)), row.color);
                float bob = Mth.sin(time * BOB_SPEED + index++ * BOB_SPREAD) * BOB_HEIGHT;
                graphics.pose().pushPose();
                graphics.pose().translate(0, bob, 0);
                graphics.drawString(font, character, x + cursor, y + row.y, row.color, false);
                graphics.pose().popPose();
                cursor += font.width(character);
            }
        }
    }

    private static float getTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private static Component rune(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withFont(GlyphStyle.RUNE.getFont()).withColor(color & 0xFFFFFF));
    }

    private static List<String> wrap(Font font, String text, int color) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(rune(candidate, color)) > MAX_LINE_WIDTH) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private record Row(String text, int color, int y) {
    }
}
