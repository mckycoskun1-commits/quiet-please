package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicSettings;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Quiet Please ayarlari: menu dugmeleri, "su an calan" yazisi, ortam sesleri, sarki arasi ve calma listesi modu.
 * Her degisiklik hemen uygulanir; ekran kapaninca config/quietplease.json dosyasina kaydedilir.
 */
public class QuietPleaseSettingsScreen extends Screen {

    private static final int BUTTON_W = 150;
    private int row = 24;
    private int groupGap = 8;

    @Nullable
    private final Screen parent;
    private GapSlider minSlider;
    private GapSlider maxSlider;

    public QuietPleaseSettingsScreen(@Nullable Screen parent) {
        super(Lang.tr("settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_W - 4;
        int right = width / 2 + 4;
        boolean compact = height < 270;
        row = compact ? 22 : 24;
        groupGap = compact ? 4 : 8;
        int y = compact ? 32 : 36;

        // Menu dugmeleri ve yazilar
        addRenderableWidget(toggle("settings.now_playing_text", ModOptions.nowPlayingText)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.now_playing_text"), (b, v) -> ModOptions.nowPlayingText = v));
        addRenderableWidget(toggle("settings.show_ambient", ModOptions.showAmbient)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.show_ambient"), (b, v) -> ModOptions.showAmbient = v));
        y += row;
        addRenderableWidget(toggle("settings.pause_button", ModOptions.pauseMenuButton)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.pause_button"), (b, v) -> ModOptions.pauseMenuButton = v));
        addRenderableWidget(toggle("settings.options_button", ModOptions.optionsButton)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.options_button"), (b, v) -> ModOptions.optionsButton = v));
        y += row;

        addRenderableWidget(toggle("settings.song_popup", ModOptions.songPopup)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.song_popup"), (b, v) -> {
                    ModOptions.songPopup = v;
                    if (v) SongPopup.preview();
                }));
        addRenderableWidget(Button.builder(Lang.tr("settings.popup_settings"), b -> {
                    SongPopup.replayPreview();
                    minecraft.setScreen(new SongPopupSettingsScreen(this));
                })
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("settings.popup_settings.tooltip")))
                .build());
        y += row;

        // Calma listesi
        addRenderableWidget(toggle("settings.playlist_mode", ModOptions.playlistMode)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.playlist_mode"), (b, v) -> ModOptions.playlistMode = v));
        addRenderableWidget(Button.builder(Lang.tr("playlist.button"), b -> minecraft.setScreen(new QuietPlaylistScreen(this)))
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("playlist.open.tooltip")))
                .build());
        y += row + groupGap;

        // Sarki arasi
        addRenderableWidget(CycleButton.<ModOptions.Gap>builder(ModOptions.Gap::displayName, ModOptions.gap)
                .withValues(ModOptions.Gap.values())
                .withTooltip(v -> Tooltip.create(Lang.tr("settings.gap." + v.id + ".tooltip")))
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.gap"), (b, v) -> {
                    ModOptions.gap = v;
                    updateSliders();
                }));
        addRenderableWidget(Button.builder(Lang.tr("settings.behavior"), b -> minecraft.setScreen(new MusicBehaviorScreen(this)))
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("settings.behavior.tooltip")))
                .build());
        y += row;
        minSlider = addRenderableWidget(new GapSlider(left, y, "settings.gap.min", ModOptions.gapMinSeconds, s -> {
            ModOptions.gapMinSeconds = s;
            if (s > ModOptions.gapMaxSeconds) maxSlider.setSeconds(s);
        }));
        maxSlider = addRenderableWidget(new GapSlider(right, y, "settings.gap.max", ModOptions.gapMaxSeconds, s -> {
            ModOptions.gapMaxSeconds = s;
            if (s < ModOptions.gapMinSeconds) minSlider.setSeconds(s);
        }));
        updateSliders();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    /** Acik/Kapali dugmesi, ipucu metni ceviriden ("...tooltip"). */
    private static CycleButton.Builder<Boolean> toggle(String key, boolean value) {
        return CycleButton.onOffBuilder(value).withTooltip(v -> Tooltip.create(Lang.tr(key + ".tooltip")));
    }

    private void updateSliders() {
        boolean custom = ModOptions.gap == ModOptions.Gap.CUSTOM;
        minSlider.active = custom;
        maxSlider.active = custom;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 14, 0xFFFFC966);
        // Sarki arasinin kisa aciklamasi, kaydiricilarin altinda
        Component hint = Lang.tr("settings.gap.hint").withStyle(ChatFormatting.GRAY);
        int hintY = maxSlider.getY() + 26;
        for (var line : font.split(hint, BUTTON_W * 2 + 8)) {
            if (hintY + font.lineHeight > height - 30) break;
            g.text(font, line, width / 2 - BUTTON_W - 4, hintY, 0xFF9A8C78, false);
            hintY += font.lineHeight;
        }
        SongPopup.render(g);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        MusicSettings.save();
    }

    /** 0-1200 sn, 5 sn adimli kaydirici. */
    private static class GapSlider extends AbstractSliderButton {
        private final String key;
        private final IntConsumer onChange;

        GapSlider(int x, int y, String key, int seconds, IntConsumer onChange) {
            super(x, y, BUTTON_W, 20, Component.empty(), seconds / (double) ModOptions.GAP_SLIDER_MAX_SECONDS);
            this.key = key;
            this.onChange = onChange;
            updateMessage();
        }

        int seconds() {
            int s = (int) Math.round(value * ModOptions.GAP_SLIDER_MAX_SECONDS / 5.0) * 5;
            return Mth.clamp(s, 0, ModOptions.GAP_SLIDER_MAX_SECONDS);
        }

        void setSeconds(int seconds) {
            value = seconds / (double) ModOptions.GAP_SLIDER_MAX_SECONDS;
            applyValue();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Lang.tr(key, formatSeconds(seconds())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(seconds());
        }
    }

    /** 75 -> "1:15", 30 -> "30 s". */
    static String formatSeconds(int seconds) {
        if (seconds < 60) return seconds + " s";
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
