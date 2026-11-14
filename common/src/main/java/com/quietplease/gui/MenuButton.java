package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.NowPlaying;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * ESC menusu (Oyun Menusu), Ayarlar ve Muzik ve Sesler ekranlarinin sag ust kosesine paneli acan
 * "♪ Quiet Please" dugmesini ekler; dugmenin hemen altinda o an calan muzigin adini kucuk yaziyla gosterir.
 */
public final class MenuButton {

    private static final int NOW_PLAYING_COLOR = 0xFF7EE08A;

    /** Su an ekranda olan dugme ve ait oldugu ekran (yazi bu dugmenin altina cizilir). */
    @Nullable
    private static Button button;
    @Nullable
    private static Screen buttonScreen;
    private static Map<ResourceLocation, MusicTrack> tracksById = Map.of();

    private MenuButton() {}

    /** Dugme bu ekrana eklensin mi? Ayarlar ekranindan ESC menusu ve Ayarlar icin ayri ayri kapatilabilir. */
    private static boolean isTarget(Screen screen) {
        if (screen instanceof PauseScreen pause) {
            return ModOptions.pauseMenuButton && pause.showsPauseMenu(); // F3+Esc menusuz duraklamada yok
        }
        return ModOptions.optionsButton && (screen instanceof OptionsScreen || screen instanceof SoundOptionsScreen);
    }

    public static void onInit(Screen screen, java.util.function.Consumer<Button> addWidget) {
        if (!isTarget(screen)) {
            if (screen == buttonScreen) button = null; // ayar kapatildiysa eski dugmenin yazisi da cizilmesin
            return;
        }
        Component label = Lang.tr("options.button");
        int w = Math.max(90, Minecraft.getInstance().font.width(label) + 16);
        button = Button.builder(label, b -> Minecraft.getInstance().setScreen(new QuietPleaseScreen(screen)))
                .bounds(screen.width - w - 6, 6, w, 20)
                .tooltip(Tooltip.create(Lang.tr("options.button.tooltip")))
                .build();
        buttonScreen = screen;
        addWidget.accept(button);
        // Parca listesi ekran her acildiginda bir kez taranir (ses kayitlari degismis olabilir, ornegin kaynak paketi)
        tracksById = MusicLibrary.scan().values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(MusicTrack::id, t -> t, (a, b) -> a));
    }

    public static void onRender(Screen screen, GuiGraphics g) {
        if (button == null || screen != buttonScreen || !ModOptions.nowPlayingText) return;
        // Sadece muzik ve plaklar; Nether ortam sesi gibi uzun dongu sesleri burada gosterilmez
        List<MusicTrack> playing = NowPlaying.find(tracksById).stream()
                .filter(t -> t.kind() != MusicTrack.Kind.OTHER).toList();
        if (playing.isEmpty()) return;

        MusicTrack track = playing.get(0);
        Font font = Minecraft.getInstance().font;
        String more = playing.size() > 1 ? "  +" + (playing.size() - 1) : "";

        // ESC menusunde: dugmenin hemen altinda (o kose bos). Ayarlar ekranlarinda alttaki satir dolu oldugu icin
        // dugmenin solunda, ayni hizada; baslik yazisina carpmayacak kadar genislikte.
        boolean below = screen instanceof PauseScreen;
        int available;
        if (below) {
            available = 200;
        } else {
            int titleRight = (screen.width + font.width(screen.getTitle())) / 2 + 8;
            available = button.getX() - 6 - titleRight;
            if (available < 40) return;
        }
        // Tam boy yazi (kucultulmus piksel yazi bulanik ve zor okunuyordu). Sigmazsa once mod adi, sonra sarki adinin
        // sonu kisalir.
        String text = "♪ " + track.shortName() + " · " + MusicLibrary.modName(track.namespace()) + more;
        if (font.width(text) > available) text = "♪ " + track.shortName() + more;
        if (font.width(text) > available) text = font.plainSubstrByWidth(text, available - font.width("…")) + "…";
        int w = font.width(text);
        int x = below ? button.getX() + button.getWidth() - w : button.getX() - 6 - w;
        int y = below ? button.getY() + button.getHeight() + 3 : button.getY() + (button.getHeight() - 8) / 2;
        g.drawString(font, text, x, y, NOW_PLAYING_COLOR);
    }
}
