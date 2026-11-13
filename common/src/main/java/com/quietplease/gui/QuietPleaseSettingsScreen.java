package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicSettings;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
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
    private static final int ROW = 24;

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
        int y = 36;

        // Menu dugmeleri ve yazilar
        addRenderableWidget(toggle("settings.now_playing_text", ModOptions.nowPlayingText)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.now_playing_text"), (b, v) -> ModOptions.nowPlayingText = v));
        addRenderableWidget(toggle("settings.show_ambient", ModOptions.showAmbient)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.show_ambient"), (b, v) -> ModOptions.showAmbient = v));
        y += ROW;
        addRenderableWidget(toggle("settings.pause_button", ModOptions.pauseMenuButton)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.pause_button"), (b, v) -> ModOptions.pauseMenuButton = v));
        addRenderableWidget(toggle("settings.options_button", ModOptions.optionsButton)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.options_button"), (b, v) -> ModOptions.optionsButton = v));
        y += ROW;

        // Calma listesi
        addRenderableWidget(toggle("settings.playlist_mode", ModOptions.playlistMode)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.playlist_mode"), (b, v) -> ModOptions.playlistMode = v));
        addRenderableWidget(Btn.builder(Lang.tr("playlist.button"), b -> minecraft.setScreen(new QuietPlaylistScreen(this)))
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("playlist.open.tooltip")))
                .build());
        y += ROW + 8;

        // Sarki arasi
        addRenderableWidget(CycleButton.<ModOptions.Gap>builder(ModOptions.Gap::displayName)
                .withValues(ModOptions.Gap.values())
                .withInitialValue(ModOptions.gap)
                .withTooltip(v -> Tooltip.lines(Lang.tr("settings.gap." + v.id + ".tooltip")))
                .create(left, y, BUTTON_W * 2 + 8, 20, Lang.tr("settings.gap"), (b, v) -> {
                    ModOptions.gap = v;
                    updateSliders();
                }));
        y += ROW;
        minSlider = addRenderableWidget(new GapSlider(left, y, "settings.gap.min", ModOptions.gapMinSeconds, s -> {
            ModOptions.gapMinSeconds = s;
            if (s > ModOptions.gapMaxSeconds) maxSlider.setSeconds(s);
        }));
        maxSlider = addRenderableWidget(new GapSlider(right, y, "settings.gap.max", ModOptions.gapMaxSeconds, s -> {
            ModOptions.gapMaxSeconds = s;
            if (s < ModOptions.gapMinSeconds) minSlider.setSeconds(s);
        }));
        updateSliders();

        addRenderableWidget(Btn.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    /** Acik/Kapali dugmesi, ipucu metni ceviriden ("...tooltip"). */
    private static CycleButton.Builder<Boolean> toggle(String key, boolean value) {
        return CycleButton.onOffBuilder(value).withTooltip(v -> Tooltip.lines(Lang.tr(key + ".tooltip")));
    }

    private void updateSliders() {
        boolean custom = ModOptions.gap == ModOptions.Gap.CUSTOM;
        minSlider.active = custom;
        maxSlider.active = custom;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(pose);
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFC966);
        // Sarki arasinin kisa aciklamasi, kaydiricilarin altinda
        Component hint = Lang.tr("settings.gap.hint").withStyle(ChatFormatting.GRAY);
        g.drawWordWrap(font, hint, width / 2 - BUTTON_W - 4, maxSlider.getY() + 28, BUTTON_W * 2 + 8, 0xFF9A8C78);
        Tooltip.renderPending(this, pose, mouseX, mouseY);
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
    private static class GapSlider extends Slider {
        private final String key;
        private final IntConsumer onChange;

        GapSlider(int x, int y, String key, int seconds, IntConsumer onChange) {
            super(x, y, BUTTON_W, 20, com.quietplease.Txt.empty(), seconds / (double) ModOptions.GAP_SLIDER_MAX_SECONDS);
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
