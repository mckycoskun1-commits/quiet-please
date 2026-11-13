package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1.19.2'de dugme ipuclari (Tooltip sinifi) yok. Ipucu metnini tutar; ekran cizildikten sonra
 * {@link #renderPending} ile en uste cizilir.
 */
public final class Tooltip {

    private static final int WIDTH = 170;

    @Nullable
    private static List<FormattedCharSequence> pending;

    final List<FormattedCharSequence> lines;

    private Tooltip(List<FormattedCharSequence> lines) {
        this.lines = lines;
    }

    public static Tooltip create(Component text) {
        return new Tooltip(Minecraft.getInstance().font.split(text, WIDTH));
    }

    /** CycleButton.withTooltip icin. */
    public static List<FormattedCharSequence> lines(Component text) {
        return create(text).lines;
    }

    public static void next(Component text) {
        pending = lines(text);
    }

    public static void next(List<FormattedCharSequence> lines) {
        pending = lines;
    }

    /** Bekleyen ipucunu (yoksa fare altindaki CycleButton'in ipucunu) cizer. */
    public static void renderPending(Screen screen, PoseStack pose, int mouseX, int mouseY) {
        renderPending(screen, pose, mouseX, mouseY, true);
    }

    /** @param scanWidgets fare altindaki CycleButton ipuclarini da ciz (kendi ekranlarimiz icin). */
    public static void renderPending(Screen screen, PoseStack pose, int mouseX, int mouseY, boolean scanWidgets) {
        List<FormattedCharSequence> lines = pending;
        pending = null;
        if (lines == null && scanWidgets) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof TooltipAccessor accessor && child instanceof AbstractWidget w
                        && w.visible && w.isMouseOver(mouseX, mouseY)) {
                    lines = accessor.getTooltip();
                    break;
                }
            }
        }
        if (lines != null && !lines.isEmpty()) screen.renderTooltip(pose, lines, mouseX, mouseY);
    }
}
