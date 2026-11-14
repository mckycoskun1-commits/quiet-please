package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicPreview;
import com.quietplease.music.MusicTrack;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SongPopup {

    private static final long SLIDE_IN_MS = 350;
    private static final long SLIDE_OUT_MS = 450;
    private static final int CONTENT_H = 16;
    private static final int TEXT_MAX = 150;
    private static final float TEXT_SCALE = 0.75F;
    private static final int MARGIN = 4;

    private static final Set<SoundInstance> KNOWN = Collections.newSetFromMap(new IdentityHashMap<>());

    private static String title = "";
    private static String subtitle = "";
    private static ItemStack icon = ItemStack.EMPTY;
    private static long shownAt = -1;
    private static long previewAt = -1;

    private SongPopup() {}

    public static void tick(Minecraft mc) {
        SoundEngine engine = ((com.quietplease.mixin.SoundManagerAccessor) mc.getSoundManager()).quietplease$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((com.quietplease.mixin.SoundEngineAccessor) engine).quietplease$getInstanceToChannel();
        ResourceLocation started = null;
        ResourceLocation startedEvent = null;
        KNOWN.retainAll(channels.keySet());
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : channels.entrySet()) {
            SoundInstance instance = entry.getKey();
            if (KNOWN.contains(instance) || entry.getValue().isStopped()) continue;
            if (instance instanceof MusicPreview.PreviewSound || instance.getSource() != SoundSource.MUSIC) continue;
            Sound sound;
            try {
                sound = instance.getSound();
            } catch (RuntimeException e) {
                continue;
            }
            if (sound == null) continue;
            KNOWN.add(instance);
            ResourceLocation file = sound.getLocation();
            if (file.equals(SoundManager.EMPTY_SOUND.getLocation())) continue;
            started = file;
            startedEvent = instance.getLocation();
        }
        if (started != null && mc.level != null && ModOptions.songPopup) show(started, startedEvent);
    }

    public static void show(ResourceLocation file, ResourceLocation event) {
        MusicTrack track = new MusicTrack(file, Set.of(event),
                file.getPath().contains("records/") ? MusicTrack.Kind.DISC : MusicTrack.Kind.MUSIC);
        show(track.shortName(), MusicLibrary.modName(track.namespace()), QuietPleaseScreen.iconFor(track));
    }

    public static void preview() {
        show(exampleTitle(), "Quiet Please", new ItemStack(Items.JUKEBOX));
    }

    public static void finishPreviewAnimation() {
        previewAt = -1;
    }

    public static void replayPreview() {
        previewAt = Util.getMillis();
    }

    public static void renderHud(GuiGraphics g) {
        Object screen = Minecraft.getInstance().screen;
        if (screen instanceof QuietPleaseSettingsScreen || screen instanceof SongPopupSettingsScreen) return;
        render(g);
    }

    private static String exampleTitle() {
        return Lang.tr("settings.song_popup.example").getString();
    }

    private static long holdMs() {
        return ModOptions.popupDuration * 1000L;
    }

    private static void show(String newTitle, String newSubtitle, ItemStack newIcon) {
        title = newTitle;
        subtitle = newSubtitle;
        icon = newIcon;
        long hold = holdMs();
        long now = Util.getMillis();
        long age = shownAt < 0 ? Long.MAX_VALUE : now - shownAt;
        if (age >= SLIDE_IN_MS && age < SLIDE_IN_MS + hold) shownAt = now - SLIDE_IN_MS;
        else if (age >= SLIDE_IN_MS + hold && age < SLIDE_IN_MS + hold + SLIDE_OUT_MS) {
            double out = (age - SLIDE_IN_MS - hold) / (double) SLIDE_OUT_MS;
            shownAt = now - (long) ((1 - out) * SLIDE_IN_MS);
        } else if (age >= SLIDE_IN_MS + hold + SLIDE_OUT_MS) shownAt = now;
    }

    public static void render(GuiGraphics g) {
        if (shownAt < 0 || !ModOptions.songPopup) return;
        if (Minecraft.getInstance().options.hideGui) return;
        double visible = visible();
        if (visible < 0) return;
        draw(g, title, subtitle, icon, visible);
    }

    public static void renderPreview(GuiGraphics g, boolean highlight) {
        long age = previewAt < 0 ? SLIDE_IN_MS : Util.getMillis() - previewAt;
        double visible = age < SLIDE_IN_MS ? easeOut(age / (double) SLIDE_IN_MS) : 1;
        Layout l = draw(g, exampleTitle(), "Quiet Please", new ItemStack(Items.JUKEBOX), visible);
        if (highlight) g.renderOutline(l.x - 2, l.y - 2, l.outerW + 4, l.outerH + 4, 0xFFFFFFFF);
    }

    public static int[] previewBounds(int screenW, int screenH) {
        Layout l = layout(Minecraft.getInstance().font, exampleTitle(), "Quiet Please", 1, screenW, screenH);
        return new int[] {l.x, l.y, l.outerW, l.outerH};
    }

    public static void placeAt(int x, int y, int screenW, int screenH) {
        int[] b = previewBounds(screenW, screenH);
        int w = b[2];
        int h = b[3];
        x = Mth.clamp(x, 0, Math.max(0, screenW - w));
        y = Mth.clamp(y, 0, Math.max(0, screenH - h));
        boolean right = x + w / 2 >= screenW / 2;
        int bestRow = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int row = 0; row < 3; row++) {
            int dist = Math.abs(y - baseY(row, h, screenH));
            if (dist < bestDist) {
                bestDist = dist;
                bestRow = row;
            }
        }
        for (ModOptions.PopupPosition p : ModOptions.PopupPosition.values()) {
            if (p.right == right && p.row == bestRow) ModOptions.popupPosition = p;
        }
        int fromEdge = right ? screenW - w - x - MARGIN : x - MARGIN;
        ModOptions.popupOffsetX = Mth.clamp(fromEdge, 0, ModOptions.POPUP_OFFSET_X_MAX);
        ModOptions.popupOffsetY = Mth.clamp(y - baseY(bestRow, h, screenH), -ModOptions.POPUP_OFFSET_Y_MAX, ModOptions.POPUP_OFFSET_Y_MAX);
    }

    private record Layout(Component name, Component from, int pad, int iconW, int textW, int w, int h,
            float scale, int outerW, int outerH, int x, int y) {}

    private static int baseY(int row, int outerH, int screenH) {
        return switch (row) {
            case 0 -> MARGIN + 2;
            case 1 -> screenH / 2 + 36;
            default -> screenH - outerH - 26;
        };
    }

    private static Layout layout(Font font, String songTitle, String songMod, double visible, int screenW, int screenH) {
        ModOptions.TextStyle style = ModOptions.popupTextStyle;
        boolean showMod = ModOptions.popupShowMod;
        Component name = styled(fit(font, songTitle, style), style);
        Component from = showMod ? styled(fit(font, songMod, style), style) : com.quietplease.Txt.empty();

        int pad = ModOptions.popupBackground == ModOptions.PopupBackground.NONE ? 0 : 3;
        int iconW = ModOptions.popupShowIcon ? 20 : 0;
        int textW = (int) Math.ceil(Math.max(font.width(name), showMod ? font.width(from) : 0) * TEXT_SCALE);
        int eqW = ModOptions.popupEqualizer ? 5 + 8 : 0;
        int w = pad + iconW + textW + eqW + pad;
        int h = CONTENT_H + pad * 2;

        float scale = ModOptions.popupScale / 100F;
        int outerW = (int) Math.ceil(w * scale);
        int outerH = (int) Math.ceil(h * scale);

        ModOptions.PopupPosition pos = ModOptions.popupPosition;
        int edge = MARGIN + ModOptions.popupOffsetX;
        int shown = pos.right ? screenW - outerW - edge : edge;
        shown = Mth.clamp(shown, 0, Math.max(0, screenW - outerW));
        int hiddenX = pos.right ? screenW + 2 : -outerW - 2;
        int x = (int) Math.round(hiddenX + (shown - hiddenX) * visible);
        int y = Mth.clamp(baseY(pos.row, outerH, screenH) + ModOptions.popupOffsetY, 0, Math.max(0, screenH - outerH));
        return new Layout(name, from, pad, iconW, textW, w, h, scale, outerW, outerH, x, y);
    }

    private static Layout draw(GuiGraphics g, String songTitle, String songMod, ItemStack songIcon, double visible) {
        Font font = Minecraft.getInstance().font;
        Layout l = layout(font, songTitle, songMod, visible, g.guiWidth(), g.guiHeight());
        boolean showMod = ModOptions.popupShowMod;

        g.pose().pushPose();
        g.pose().translate(l.x, l.y, 0);
        g.pose().scale(l.scale, l.scale, 1.0F);
        drawBackground(g, l.w, l.h);
        if (ModOptions.popupShowIcon) g.renderItem(songIcon, l.pad, l.pad);
        int textX = l.pad + l.iconW;
        g.pose().pushPose();
        g.pose().translate(textX, l.pad + (showMod ? 2 : 5), 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        g.drawString(font, l.name, 0, 0, ModOptions.popupTitleColor.argb(), ModOptions.popupShadow);
        if (showMod) g.drawString(font, l.from, 0, 10, ModOptions.popupModColor.argb(), ModOptions.popupShadow);
        g.pose().popPose();
        if (ModOptions.popupEqualizer) QuietPleaseScreen.drawEqualizer(g, textX + l.textW + 5, l.pad + 14, QuietPleaseScreen.PLAYING);
        g.pose().popPose();
        return l;
    }

    private static void drawBackground(GuiGraphics g, int w, int h) {
        switch (ModOptions.popupBackground) {
            case NONE -> {}
            case SOFT -> g.fill(0, 0, w, h, 0x50000000);
            case DARK -> g.fill(0, 0, w, h, 0xC0101010);
            case FRAMED -> {
                g.fill(0, 0, w, h, 0xC0101010);
                g.renderOutline(0, 0, w, h, 0x80000000 | ModOptions.popupTitleColor.rgb);
            }
        }
    }

    private static Component styled(String s, ModOptions.TextStyle style) {
        return com.quietplease.Txt.literal(s).withStyle(st -> st.withBold(style.bold).withItalic(style.italic));
    }

    private static String fit(Font font, String s, ModOptions.TextStyle style) {
        if (font.width(styled(s, style)) <= TEXT_MAX) return s;
        String cut = font.plainSubstrByWidth(s, TEXT_MAX);
        while (!cut.isEmpty() && font.width(styled(cut + "…", style)) > TEXT_MAX) cut = cut.substring(0, cut.length() - 1);
        return cut + "…";
    }

    private static double visible() {
        long hold = holdMs();
        long age = Util.getMillis() - shownAt;
        if (age < SLIDE_IN_MS) return easeOut(age / (double) SLIDE_IN_MS);
        if (age < SLIDE_IN_MS + hold) return 1;
        if (age < SLIDE_IN_MS + hold + SLIDE_OUT_MS) return 1 - easeIn((age - SLIDE_IN_MS - hold) / (double) SLIDE_OUT_MS);
        shownAt = -1;
        return -1;
    }

    private static double easeOut(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    private static double easeIn(double t) {
        return t * t * t;
    }
}
