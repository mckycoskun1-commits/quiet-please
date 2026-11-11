package com.quietplease;

import com.mojang.logging.LogUtils;
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
        NeoForge.EVENT_BUS.addListener(QuietPlease::addOptionsButton);

        // Mods listesinde Quiet Please > "Config" dugmesi paneli acar
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new QuietPleaseScreen(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Quiet Please yuklendi.");
    }

    /**
     * Ayarlar (Options) ve Muzik ve Sesler (Music & Sounds) ekranlarinin sag ust kosesine
     * paneli acan bir "Quiet Please" dugmesi ekler.
     */
    private static void addOptionsButton(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof OptionsScreen) && !(screen instanceof SoundOptionsScreen)) return;
        Component label = Lang.tr("options.button");
        int w = Math.max(90, Minecraft.getInstance().font.width(label) + 16);
        event.addListener(Button.builder(label,
                        b -> Minecraft.getInstance().setScreen(new QuietPleaseScreen(screen)))
                .bounds(screen.width - w - 6, 6, w, 20)
                .tooltip(Tooltip.create(Lang.tr("options.button.tooltip")))
                .build());
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
        var tracks = com.quietplease.music.MusicLibrary.scan().get("minecraft");
        switch (guiTestTicks) {
            case 100 -> {
                MusicSettings.setEnabled(tracks.get(2).id(), false);
                MusicSettings.setVolume(tracks.get(3).id(), 40);
                mc.setScreen(new QuietPleaseScreen(new TitleScreen()));
            }
            case 118 -> {
                LOGGER.info("GUITEST muzik ses seviyesi: {}", mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC));
                shot(mc, "quietplease-panel.png"); // "su an calan" cubugu, onizleme yok
            }
            case 120 -> {
                mc.getSoundManager().pause(); // ESC ile duraklatilmis oyunu taklit et: calan sesler bekler
                LOGGER.info("GUITEST onizleme basladi: {}", com.quietplease.music.MusicPreview.play(tracks.get(1)));
            }
            case 140 -> {
                LOGGER.info("GUITEST onizleme (duraklatilmisken) caliyor mu: {}", com.quietplease.music.MusicPreview.isActive());
                shot(mc, "quietplease-preview.png");
            }
            case 145 -> {
                mc.getSoundManager().resume();
                MusicSettings.reset(tracks.get(2).id());
                MusicSettings.reset(tracks.get(3).id());
                mc.setScreen(new OptionsScreen(new TitleScreen(), mc.options)); // panel kapanir ve kaydeder
            }
            case 160 -> shot(mc, "quietplease-options.png");
            case 165 -> guiTestTicks = -1;
            default -> { }
        }
    }

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
