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
    private static final int HEIGHT = 16;
    private static final int TEXT_MAX = 150;
    private static final float TEXT_SCALE = 0.75F;
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
        double visible = visible();
        if (visible < 0) return;

        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        String name = fit(font, title, TEXT_MAX);
        String from = fit(font, subtitle, TEXT_MAX);
        int textW = (int) Math.ceil(Math.max(font.width(name), font.width(from)) * TEXT_SCALE);
        int w = 20 + textW + 5 + 8;

        ModOptions.PopupPosition pos = ModOptions.popupPosition;
        int hidden = (int) Math.round((1 - visible) * (w + MARGIN + 2));
        int x = pos.right ? screenW - w - MARGIN + hidden : MARGIN - hidden;
        int y = top(pos, screenH);

        g.item(icon, x, y);
        int textX = x + 20;
        g.pose().pushMatrix();
        g.pose().translate(textX, y + 2);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE);
        g.text(font, name, 0, 0, QuietPleaseScreen.GOLD, true);
        g.text(font, from, 0, 10, 0xFFD8CCB4, true);
        g.pose().popMatrix();
        QuietPleaseScreen.drawEqualizer(g, textX + textW + 5, y + 14, QuietPleaseScreen.PLAYING);
    }

    private static int top(ModOptions.PopupPosition pos, int screenH) {
        return switch (pos) {
            case RIGHT_MIDDLE, LEFT_MIDDLE -> screenH / 2 + 36;
            case RIGHT_BOTTOM -> screenH - HEIGHT - 26;
            case LEFT_TOP -> MARGIN + 2;
        };
    }

    private static double visible() {
        long age = Util.getMillis() - shownAt;
        if (age < SLIDE_IN_MS) return easeOut(age / (double) SLIDE_IN_MS);
        if (age < SLIDE_IN_MS + HOLD_MS) return 1;
        if (age < SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS) return 1 - easeIn((age - SLIDE_IN_MS - HOLD_MS) / (double) SLIDE_OUT_MS);
        shownAt = -1;
        return -1;
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
