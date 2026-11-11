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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Quiet Please ana sinifi.
 *
 * Mod sadece istemci tarafinda (oyuncunun bilgisayarinda) calisir: dist = Dist.CLIENT
 * oldugu icin sunucuda bu sinif hic yuklenmez.
 */
@Mod(value = QuietPlease.MODID, dist = Dist.CLIENT)
public class QuietPlease {
    public static final String MODID = "quietplease";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean autoDumpDone = false;

    public QuietPlease(IEventBus modEventBus, ModContainer container) {
        MusicSettings.load();
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(QuietPleaseCommands::register);
        NeoForge.EVENT_BUS.addListener(QuietPlease::onClientTick);
        NeoForge.EVENT_BUS.addListener(QuietPlease::onScreenInit);
        NeoForge.EVENT_BUS.addListener(MenuButton::onInit);
        NeoForge.EVENT_BUS.addListener(MenuButton::onRender);

        // Mods listesinde Quiet Please > "Config" dugmesi paneli acar
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new QuietPleaseScreen(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Quiet Please yuklendi.");
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (guiTestTicks >= 0) guiTestTick(mc);
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
                if (t == 60) mc.getMusicManager().startPlaying(net.minecraft.sounds.Musics.GAME);
                else if (t == 100) mc.pauseGame(false);             // ESC
                else if (t == 120) shot(mc, "quietplease-pause.png");
                else if (t == 122) mc.setScreen(new QuietPleaseScreen(mc.screen));
                else if (t == 140) {
                    LOGGER.info("GUITEST oyun ici panel: oyun duraklatildi mi: {}", mc.isPaused());
                    shot(mc, "quietplease-ingame.png");
                } else if (t == 145) guiTestTicks = -1;
            }
        }
    }

    private static int worldJoinTick = -1;

    private static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                msg -> LOGGER.info("GUITEST ekran goruntusu: {}", msg.getString()));
    }

    /**
     * Gelistirme/test kolayligi: QUIETPLEASE_AUTODUMP ortam degiskeni ayarliysa, ana menu ilk acildiginda
     * (tum sesler yuklenmis olur) oz-testi calistirir ve muzik listesini quietplease-tracks.txt dosyasina yazar.
     * QUIETPLEASE_AUTODUMP=gui ise Ayarlar ekranlarinin ekran goruntulerini de alir.
     */
    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (FMLEnvironment.production) return; // gercek oyunda (launcher'dan acilinca) hic calismaz
        String mode = System.getenv("QUIETPLEASE_AUTODUMP");
        if (autoDumpDone || !(event.getScreen() instanceof TitleScreen) || mode == null) return;
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
