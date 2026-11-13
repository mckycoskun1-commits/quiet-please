package com.quietplease;

import com.mojang.logging.LogUtils;
import com.quietplease.gui.MenuButton;
import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.music.MusicSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class QuietPlease {
    public static final String MODID = "quietplease";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean autoDumpDone = false;

    public static void init() {
        MusicSettings.load();
        LOGGER.info("Quiet Please loaded.");
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        com.quietplease.music.Playlist.clientTick(mc);
        QuietPleaseKeys.tick(mc);
        if (guiTestTicks >= 0) guiTestTick(mc);
        if (!autoDumpDone && mc.screen instanceof TitleScreen && mc.getOverlay() == null) onScreenInit(mc.screen);
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
                && System.getenv("QUIETPLEASE_AUTODUMP") != null
                && com.quietplease.platform.Services.PLATFORM.isDevelopment()) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen(true));
        }
    }

    /**
     * Gelistirme testi (QUIETPLEASE_AUTODUMP=gui): paneli acar, ornek ayarlar ve bir onizlemeyle
     * ekran goruntusu alir, sonra ornek ayarlari geri alir.
     */
    private static int guiTestTicks = -1;

    private static void guiTestTick(Minecraft mc) {
        guiTestTicks++;
        var tracks = guiTestTicks <= 180 ? com.quietplease.music.MusicLibrary.scan().get("minecraft") : null;
        switch (guiTestTicks) {
            case 100 -> {
                MusicSettings.setEnabled(tracks.get(2).id(), false);
                MusicSettings.setVolume(tracks.get(3).id(), 40);
                mc.setScreen(new QuietPleaseScreen(new TitleScreen()));
            }
            case 108 -> {
                // Mod filtresine sag tik: "Tumu"den bir geri -> listenin son modu olmali
                int w = mc.getWindow().getGuiScaledWidth();
                int rowW = Math.min(w - 20, 420);
                double x = (w - rowW) / 2.0 + (rowW - 150 - 96 - 8) + 4 + 75;
                mc.screen.mouseClicked(x, 42, 1);
            }
            case 112 -> shot(mc, "quietplease-modfilter.png");
            case 114 -> mc.setScreen(new QuietPleaseScreen(new TitleScreen())); // filtre sifirlansin
            case 118 -> {
                LOGGER.info("GUITEST muzik ses seviyesi: {}", mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC));
                shot(mc, "quietplease-panel.png"); // "su an calan" cubugu, onizleme yok
            }
            // Menu muzigi calarken: Ayarlar ve ESC menusundeki dugme + altindaki "calan sarki" yazisi
            case 120 -> mc.setScreen(new OptionsScreen(new TitleScreen(), mc.options));
            case 135 -> shot(mc, "quietplease-options.png");
            // Ayarlar ekrani
            case 136 -> mc.setScreen(new com.quietplease.gui.QuietPleaseSettingsScreen(new TitleScreen()));
            case 141 -> shot(mc, "quietplease-settings.png");
            // Calma listesi: birkac ornek sarki, ilki hemen calsin (ilerleme cubugu gorunsun)
            case 142 -> {
                playlistBackup = com.quietplease.music.Playlist.snapshot();
                playlistModeBackup = com.quietplease.music.ModOptions.playlistMode;
                com.quietplease.music.Playlist.clear();
                for (int i : new int[] {4, 9, 1, 12, 6}) {
                    if (i < tracks.size()) com.quietplease.music.Playlist.add(tracks.get(i).id());
                }
                // Dongu dugmesi ve Favoriler grubu gorunsun
                if (tracks.size() > 12) {
                    com.quietplease.music.Playlist.toggleLoop(tracks.get(9).id());
                    for (int i : new int[] {4, 12}) {
                        if (!com.quietplease.music.Playlist.isFavorite(tracks.get(i).id())) {
                            com.quietplease.music.Playlist.toggleFavorite(tracks.get(i).id());
                        }
                    }
                }
                com.quietplease.music.ModOptions.playlistMode = true;
                com.quietplease.music.Playlist.playNow(0);
                mc.setScreen(new com.quietplease.gui.QuietPlaylistScreen(new TitleScreen()));
            }
            case 150 -> {
                var playing = com.quietplease.music.Playlist.playing();
                boolean ok = playing != null && playing.trackId.equals(com.quietplease.music.Playlist.tracks().get(0));
                LOGGER.info("GUITEST calma listesi menude: {} ({})", ok ? "BASARILI" : "HATA", playing == null ? "calmiyor" : playing.trackId);
                shot(mc, "quietplease-playlist.png");
            }
            case 151 -> mc.setScreen(new QuietPleaseScreen(new TitleScreen()));
            case 154 -> shot(mc, "quietplease-favorites.png");
            // (ESC menusu test edilemiyor: PauseScreen bir dunya/oyuncu ister, ana menude acilinca coker)
            // Onizleme (arka plan muzigini susturur, o yuzden en sona)
            case 155 -> {
                mc.setScreen(new QuietPleaseScreen(new TitleScreen()));
                mc.getSoundManager().pause(); // ESC ile duraklatilmis oyunu taklit et: calan sesler bekler
                LOGGER.info("GUITEST onizleme basladi: {}", com.quietplease.music.MusicPreview.play(tracks.get(1)));
            }
            case 175 -> {
                LOGGER.info("GUITEST onizleme (duraklatilmisken) caliyor mu: {}", com.quietplease.music.MusicPreview.isActive());
                shot(mc, "quietplease-preview.png");
            }
            case 180 -> {
                mc.getSoundManager().resume();
                MusicSettings.reset(tracks.get(2).id());
                MusicSettings.reset(tracks.get(3).id());
                // Ikinci asama: bir deneme dunyasi olusturup ESC menusunu ve oyun ici paneli dene
                var settings = new net.minecraft.world.level.LevelSettings("qptest-" + System.currentTimeMillis(),
                        net.minecraft.world.level.GameType.CREATIVE, false, net.minecraft.world.Difficulty.PEACEFUL, true,
                        new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings,
                        new net.minecraft.world.level.levelgen.WorldOptions(0L, false, false),
                        net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions, new TitleScreen());
                worldJoinTick = -1;
            }
            default -> {
                if (guiTestTicks <= 180) return;
                if (worldJoinTick < 0) {
                    if (mc.player != null && mc.level != null) worldJoinTick = guiTestTicks;
                    else if (guiTestTicks > 180 + 20 * 120) { LOGGER.error("GUITEST dunya 2 dakikada acilmadi"); guiTestTicks = -1; }
                    return;
                }
                int t = guiTestTicks - worldJoinTick;
                if (t == 60) {
                    // Calma listesi modu acik: oyunun "oyun muzigi" yerine listedeki siradaki parca calmali
                    expectedTrack = nextUpcoming();
                    mc.getMusicManager().stopPlaying();
                    mc.getMusicManager().startPlaying(net.minecraft.sounds.Musics.GAME);
                } else if (t == 70 || t == 85) {
                    var playing = com.quietplease.music.Playlist.playing();
                    boolean ok = playing != null && playing.trackId.equals(expectedTrack)
                            && expectedTrack.equals(com.quietplease.music.Playlist.currentTrack())
                            && (t == 70 || !com.quietplease.music.Playlist.contains(skippedTrack)
                                    || com.quietplease.music.Playlist.isLooping(skippedTrack));
                    LOGGER.info("GUITEST calma listesi {}: {} (beklenen {}, calan {})", t == 70 ? "oyun muzigi yerine" : "Atla sonrasi",
                            ok ? "BASARILI" : "HATA", expectedTrack, playing == null ? "yok" : playing.trackId);
                    if (t == 70) {
                        skippedTrack = expectedTrack;
                        expectedTrack = nextUpcoming();
                        com.quietplease.music.MusicControl.skip();
                    }
                } else if (t == 100) mc.pauseGame(false);             // ESC
                else if (t == 120) shot(mc, "quietplease-pause.png");
                else if (t == 122) mc.setScreen(new QuietPleaseScreen(mc.screen));
                else if (t == 140) {
                    LOGGER.info("GUITEST oyun ici panel: oyun duraklatildi mi: {}", mc.isPaused());
                    shot(mc, "quietplease-ingame.png");
                } else if (t == 145) {
                    // Ornek calma listesini geri al
                    if (playlistBackup != null) com.quietplease.music.Playlist.restore(playlistBackup);
                    com.quietplease.music.ModOptions.playlistMode = playlistModeBackup;
                    MusicSettings.save();
                    LOGGER.info("GUITEST bitti");
                    guiTestTicks = -1;
                }
            }
        }
    }

    private static int worldJoinTick = -1;
    private static net.minecraft.resources.ResourceLocation expectedTrack;
    private static net.minecraft.resources.ResourceLocation skippedTrack;

    private static net.minecraft.resources.ResourceLocation nextUpcoming() {
        var order = com.quietplease.music.Playlist.upcoming();
        return order.isEmpty() ? null : com.quietplease.music.Playlist.tracks().get(order.get(0));
    }
    private static com.google.gson.JsonObject playlistBackup;
    private static boolean playlistModeBackup;

    private static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                msg -> LOGGER.info("GUITEST ekran goruntusu: {}", msg.getString()));
    }

    /**
     * Gelistirme/test kolayligi: QUIETPLEASE_AUTODUMP ortam degiskeni ayarliysa, ana menu ilk acildiginda
     * (tum sesler yuklenmis olur) oz-testi calistirir ve muzik listesini quietplease-tracks.txt dosyasina yazar.
     * QUIETPLEASE_AUTODUMP=gui ise Ayarlar ekranlarinin ekran goruntulerini de alir.
     */
    public static void onScreenInit(Screen screen) {
        if (!com.quietplease.platform.Services.PLATFORM.isDevelopment()) return;
        String mode = System.getenv("QUIETPLEASE_AUTODUMP");
        if (autoDumpDone || !(screen instanceof TitleScreen) || mode == null || Minecraft.getInstance().getOverlay() != null) return;
        autoDumpDone = true;
        try {
            DevSelfTest.run();
            LOGGER.info("QUIETPLEASE_AUTODUMP: liste yazildi -> {}", QuietPleaseCommands.writeDump().toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("QUIETPLEASE_AUTODUMP basarisiz", e);
        }
        if (mode.equals("gui")) guiTestTicks = 0;
    }
}
