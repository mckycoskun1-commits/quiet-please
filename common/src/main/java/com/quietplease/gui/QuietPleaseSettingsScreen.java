package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.Loudness;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicMixer;
import com.quietplease.music.MusicSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Quiet Please ayarlari, uc sekmede:
 * Muzik (calma listesi, kendi muzigin, gecmis, sarki arasi), Davranis (muzigin ne zaman kisilacagi, gecisler, ses
 * esitleme, Alt+Tab'da sessiz) ve Ekran (dugmeler, yazilar, sarki bildirimi).
 * Her degisiklik hemen uygulanir; ekran kapaninca config/quietplease.json dosyasina kaydedilir.
 */
public class QuietPleaseSettingsScreen extends Screen {

    public enum Tab {
        MUSIC("music"),
        BEHAVIOR("behavior"),
        DISPLAY("display");

        final String id;

        Tab(String id) {
            this.id = id;
        }
    }

    private static final int BUTTON_W = 150;
    private static final int FULL_W = BUTTON_W * 2 + 8;
    private static final int TAB_W = 100;
    /** Bir alt ekrandan ya da paneli yeniden acinca son bakilan sekme. */
    private static Tab lastTab = Tab.MUSIC;

    @Nullable
    private final Screen parent;
    private Tab tab;
    private int row = 24;
    private int contentTop;
    @Nullable
    private GapSlider minSlider;
    @Nullable
    private GapSlider maxSlider;
    private int gapHeaderY = -1;
    private int hintY = -1;
    private int statusY = -1;

    public QuietPleaseSettingsScreen(@Nullable Screen parent) {
        this(parent, lastTab);
    }

    public QuietPleaseSettingsScreen(@Nullable Screen parent, Tab tab) {
        super(Lang.tr("settings.title"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        lastTab = tab;
        minSlider = null;
        maxSlider = null;
        gapHeaderY = -1;
        hintY = -1;
        statusY = -1;
        boolean compact = height < 270;
        row = compact ? 22 : 24;

        // Sekmeler: secili olan basili (pasif) gorunur
        int tabsLeft = width / 2 - (TAB_W * 3 + 8) / 2;
        for (Tab t : Tab.values()) {
            Button b = Button.builder(Lang.tr("settings.tab." + t.id), x -> switchTo(t))
                    .bounds(tabsLeft + t.ordinal() * (TAB_W + 4), compact ? 26 : 30, TAB_W, 20)
                    .tooltip(Tooltip.create(Lang.tr("settings.tab." + t.id + ".tooltip")))
                    .build();
            b.active = t != tab;
            addRenderableWidget(b);
        }
        contentTop = (compact ? 26 : 30) + 20 + (compact ? 18 : 22);

        int left = width / 2 - BUTTON_W - 4;
        int right = width / 2 + 4;
        switch (tab) {
            case MUSIC -> initMusic(left, right, compact);
            case BEHAVIOR -> initBehavior(left, right);
            case DISPLAY -> initDisplay(left, right);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void switchTo(Tab t) {
        tab = t;
        rebuildWidgets();
    }

    private void initMusic(int left, int right, boolean compact) {
        int y = contentTop;
        addRenderableWidget(toggle("settings.playlist_mode", ModOptions.playlistMode)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.playlist_mode"), (b, v) -> ModOptions.playlistMode = v));
        addRenderableWidget(Button.builder(Lang.tr("playlist.button"), b -> minecraft.setScreen(new QuietPlaylistScreen(this)))
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("playlist.open.tooltip")))
                .build());
        y += row;
        addRenderableWidget(toggle("behavior.custom_music", ModOptions.customInBackground)
                .create(left, y, BUTTON_W, 20, Lang.tr("behavior.custom_music"), (b, v) -> ModOptions.customInBackground = v));
        addRenderableWidget(Button.builder(Lang.tr("settings.custom_folder"), b -> {
                    com.quietplease.music.CustomMusic.refresh();
                    net.minecraft.Util.getPlatform().openFile(com.quietplease.music.CustomMusic.folder().toFile());
                })
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("settings.custom_folder.tooltip")))
                .build());
        y += row;
        addRenderableWidget(Button.builder(Lang.tr("settings.history"),
                        b -> minecraft.setScreen(new ListenHistoryScreen(this, ListenHistoryScreen.Tab.RECENT)))
                .bounds(left, y, FULL_W, 20)
                .tooltip(Tooltip.create(Lang.tr("settings.history.tooltip")))
                .build());
        y += row + (compact ? 4 : 8);

        // Sarki arasi: baslik, secim, kaydiricilar, aciklama
        gapHeaderY = y;
        y += 12;
        addRenderableWidget(CycleButton.<ModOptions.Gap>builder(ModOptions.Gap::displayName).withInitialValue(ModOptions.gap)
                .withValues(ModOptions.Gap.values())
                .withTooltip(v -> Tooltip.create(Lang.tr("settings.gap." + v.id + ".tooltip").append("\n\n").append(Lang.tr("settings.gap.hint"))))
                .create(left, y, FULL_W, 20, Lang.tr("settings.gap"), (b, v) -> {
                    ModOptions.gap = v;
                    updateSliders();
                }));
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
        hintY = y + 24;
    }

    private void initBehavior(int left, int right) {
        int y = contentTop;
        addRenderableWidget(choice(left, y, "behavior.jukebox", ModOptions.JUKEBOX_VALUES, ModOptions.jukeboxVolume,
                QuietPleaseSettingsScreen::volumeName, v -> ModOptions.jukeboxVolume = v));
        addRenderableWidget(choice(right, y, "behavior.combat", ModOptions.COMBAT_VALUES, ModOptions.combatVolume,
                v -> v >= 100 ? Lang.tr("behavior.off") : Lang.tr("behavior.combat." + v), v -> ModOptions.combatVolume = v));
        y += row;
        addRenderableWidget(choice(left, y, "behavior.unfocused", ModOptions.UNFOCUSED_VALUES, ModOptions.unfocusedVolume,
                QuietPleaseSettingsScreen::volumeName, v -> ModOptions.unfocusedVolume = v));
        addRenderableWidget(toggle("behavior.mute_all_unfocused", ModOptions.muteAllUnfocused)
                .create(right, y, BUTTON_W, 20, Lang.tr("behavior.mute_all_unfocused"), (b, v) -> ModOptions.muteAllUnfocused = v));
        y += row;
        addRenderableWidget(choice(left, y, "behavior.fade", ModOptions.FADE_VALUES, ModOptions.fadeSeconds,
                v -> v <= 0 ? Lang.tr("behavior.off") : Lang.tr("behavior.seconds", v), v -> ModOptions.fadeSeconds = v));
        addRenderableWidget(toggle("settings.fade_skip", ModOptions.fadeOnSkip)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.fade_skip"), (b, v) -> ModOptions.fadeOnSkip = v));
        y += row;
        addRenderableWidget(choice(left, y, "behavior.no_repeat", ModOptions.NO_REPEAT_VALUES, ModOptions.noRepeat,
                v -> v <= 0 ? Lang.tr("behavior.off") : Lang.tr("behavior.last", v), v -> {
                    ModOptions.noRepeat = v;
                    MusicMixer.optionsChanged();
                }));
        addRenderableWidget(choice(right, y, "behavior.favorites", ModOptions.FAVORITE_VALUES, ModOptions.favoriteWeight,
                v -> v <= 1 ? Lang.tr("behavior.off") : Lang.tr("behavior.times", v), v -> ModOptions.favoriteWeight = v));
        y += row;
        addRenderableWidget(toggle("behavior.normalize", ModOptions.normalize)
                .create(left, y, FULL_W, 20, Lang.tr("behavior.normalize"), (b, v) -> {
                    ModOptions.normalize = v;
                    MusicSettings.applyToPlayingSounds();
                }));
        statusY = y + 26;
    }

    private void initDisplay(int left, int right) {
        int y = contentTop;
        addRenderableWidget(toggle("settings.pause_button", ModOptions.pauseMenuButton)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.pause_button"), (b, v) -> ModOptions.pauseMenuButton = v));
        addRenderableWidget(toggle("settings.options_button", ModOptions.optionsButton)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.options_button"), (b, v) -> ModOptions.optionsButton = v));
        y += row;
        addRenderableWidget(toggle("settings.now_playing_text", ModOptions.nowPlayingText)
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.now_playing_text"), (b, v) -> ModOptions.nowPlayingText = v));
        addRenderableWidget(toggle("settings.show_ambient", ModOptions.showAmbient)
                .create(right, y, BUTTON_W, 20, Lang.tr("settings.show_ambient"), (b, v) -> ModOptions.showAmbient = v));
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
    }

    /** Acik/Kapali dugmesi, ipucu metni ceviriden ("...tooltip"). */
    private static CycleButton.Builder<Boolean> toggle(String key, boolean value) {
        return CycleButton.onOffBuilder(value).withTooltip(v -> Tooltip.create(Lang.tr(key + ".tooltip")));
    }

    private static Component volumeName(int v) {
        if (v >= 100) return Lang.tr("behavior.off");
        if (v <= 0) return Lang.tr("behavior.mute");
        return Lang.tr("behavior.lower", v);
    }

    private static CycleButton<Integer> choice(int x, int y, String key, int[] values, int current,
            IntFunction<Component> name, IntConsumer onChange) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int v : values) list.add(v);
        return CycleButton.<Integer>builder(name::apply).withInitialValue(current)
                .withValues(list)
                .withTooltip(v -> Tooltip.create(Lang.tr(key + ".tooltip")))
                .create(x, y, BUTTON_W, 20, Lang.tr(key), (b, v) -> onChange.accept(v));
    }

    private void updateSliders() {
        if (minSlider == null || maxSlider == null) return;
        boolean custom = ModOptions.gap == ModOptions.Gap.CUSTOM;
        minSlider.active = custom;
        maxSlider.active = custom;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFC966);
        // Secili sekmenin ne ise yaradigi
        int descY = contentTop - (height < 270 ? 14 : 16);
        g.drawCenteredString(font, Lang.tr("settings.tab." + tab.id + ".description"), width / 2, descY, 0xFFB8AC98);
        int left = width / 2 - BUTTON_W - 4;
        if (gapHeaderY >= 0) {
            g.drawString(font, Lang.tr("settings.gap.header"), left, gapHeaderY, 0xFFFFC966, false);
        }
        if (hintY >= 0) {
            // Aciklama ya tamamen sigar ya hic gosterilmez (yarim cumle kalmasin); ayni metin Sarki arasi ipucunda da var
            var lines = font.split(Lang.tr("settings.gap.hint").withStyle(ChatFormatting.GRAY), FULL_W);
            if (hintY + lines.size() * font.lineHeight <= height - 32) {
                int y = hintY;
                for (var line : lines) {
                    g.drawString(font, line, left, y, 0xFF9A8C78, false);
                    y += font.lineHeight;
                }
            }
        }
        // Secili sekmenin altinda altin cizgi
        int tabY = (height < 270 ? 26 : 30) + 20;
        int tabX = width / 2 - (TAB_W * 3 + 8) / 2 + tab.ordinal() * (TAB_W + 4);
        g.fill(tabX + 2, tabY, tabX + TAB_W - 2, tabY + 2, 0xFFFFC966);
        if (statusY >= 0 && ModOptions.normalize && Loudness.pending() > 0 && statusY + font.lineHeight <= height - 30) {
            g.drawCenteredString(font, Lang.tr("behavior.normalize.measuring", Loudness.pending()), width / 2, statusY, 0xFFA0A0A0);
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
