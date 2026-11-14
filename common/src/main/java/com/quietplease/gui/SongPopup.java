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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SongPopup {

    private static final long SLIDE_IN_MS = 350;
    private static final long HOLD_MS = 4000;
    private static final long SLIDE_OUT_MS = 450;
    private static final int HEIGHT = 26;
    private static final int MAX_WIDTH = 170;
    private static final int MARGIN = 4;

    private static final Set<SoundInstance> KNOWN = Collections.newSetFromMap(new IdentityHashMap<>());

    private static String title = "";
    private static String subtitle = "";
    private static ItemStack icon = ItemStack.EMPTY;
    private static long shownAt = -1;

    private SongPopup() {}

    public static void tick(Minecraft mc) {
        SoundEngine engine = ((com.quietplease.mixin.SoundManagerAccessor) mc.getSoundManager()).quietplease$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((com.quietplease.mixin.SoundEngineAccessor) engine).quietplease$getInstanceToChannel();
        Identifier started = null;
        Identifier startedEvent = null;
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
            Identifier file = sound.getLocation();
            if (file.equals(SoundManager.EMPTY_SOUND_LOCATION) || file.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION)) continue;
            started = file;
            startedEvent = instance.getIdentifier();
        }
        if (started != null && mc.level != null && ModOptions.songPopup) show(started, startedEvent);
    }

    public static void show(Identifier file, Identifier event) {
        MusicTrack track = new MusicTrack(file, Set.of(event),
                file.getPath().contains("records/") ? MusicTrack.Kind.DISC : MusicTrack.Kind.MUSIC);
        show(track.shortName(), MusicLibrary.modName(track.namespace()), QuietPleaseScreen.iconFor(track));
    }

    public static void preview() {
        show(Lang.tr("settings.song_popup.example").getString(), "Quiet Please", QuietPleaseScreen.stack(Items.JUKEBOX));
    }

    public static void renderHud(GuiGraphicsExtractor g) {
        if (Minecraft.getInstance().gui.screen() instanceof QuietPleaseSettingsScreen) return;
        render(g);
    }

    private static void show(String newTitle, String newSubtitle, ItemStack newIcon) {
        title = newTitle;
        subtitle = newSubtitle;
        icon = newIcon;
        long now = Util.getMillis();
        long age = shownAt < 0 ? Long.MAX_VALUE : now - shownAt;
        if (age >= SLIDE_IN_MS && age < SLIDE_IN_MS + HOLD_MS) shownAt = now - SLIDE_IN_MS;
        else if (age >= SLIDE_IN_MS + HOLD_MS && age < SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS) {
            double out = (age - SLIDE_IN_MS - HOLD_MS) / (double) SLIDE_OUT_MS;
            shownAt = now - (long) ((1 - out) * SLIDE_IN_MS);
        } else if (age >= SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS) shownAt = now;
    }

    public static void render(GuiGraphicsExtractor g) {
        if (shownAt < 0 || !ModOptions.songPopup) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.hud.isHidden()) return;
        long age = Util.getMillis() - shownAt;
        double visible;
        if (age < SLIDE_IN_MS) visible = easeOut(age / (double) SLIDE_IN_MS);
        else if (age < SLIDE_IN_MS + HOLD_MS) visible = 1;
        else if (age < SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS) visible = 1 - easeIn((age - SLIDE_IN_MS - HOLD_MS) / (double) SLIDE_OUT_MS);
        else {
            shownAt = -1;
            return;
        }

        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int textX = 25;
        int eqW = 12;
        int textMax = MAX_WIDTH - textX - eqW - 8;
        String name = fit(font, title, textMax);
        String from = fit(font, subtitle, textMax);
        int w = textX + Math.max(font.width(name), font.width(from)) + eqW + 8;

        ModOptions.PopupPosition pos = ModOptions.popupPosition;
        boolean right = pos.right;
        int hidden = (int) Math.round((1 - visible) * (w + MARGIN + 2));
        int x = right ? screenW - w - MARGIN + hidden : MARGIN - hidden;
        int y = switch (pos) {
            case RIGHT_MIDDLE, LEFT_MIDDLE -> (screenH - HEIGHT) / 2 + 24;
            case RIGHT_BOTTOM -> screenH - HEIGHT - 30;
            case LEFT_TOP -> MARGIN + 2;
        };

        g.fill(x + 1, y, x + w - 1, y + HEIGHT, 0xD8140F0A);
        g.fill(x, y + 1, x + w, y + HEIGHT - 1, 0xD8140F0A);
        g.fill(x + 1, y, x + w - 1, y + 1, 0x60E89A3C);
        g.fill(x + 1, y + HEIGHT - 1, x + w - 1, y + HEIGHT, 0x40000000);
        int stripeX = right ? x : x + w - 2;
        g.fill(stripeX, y + 2, stripeX + 2, y + HEIGHT - 2, QuietPleaseScreen.ACCENT);

        int inner = right ? x + 3 : x;
        g.item(icon, inner + 4, y + (HEIGHT - 16) / 2);
        g.text(font, name, inner + textX, y + 4, QuietPleaseScreen.GOLD, false);
        g.text(font, from, inner + textX, y + 14, QuietPleaseScreen.MUTED, false);
        QuietPleaseScreen.drawEqualizer(g, inner + w - eqW - 4, y + HEIGHT - 6, QuietPleaseScreen.PLAYING);
    }

    private static String fit(Font font, String s, int max) {
        if (font.width(s) <= max) return s;
        return font.plainSubstrByWidth(s, max - font.width("…")) + "…";
    }

    private static double easeOut(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    private static double easeIn(double t) {
        return t * t * t;
    }
}
