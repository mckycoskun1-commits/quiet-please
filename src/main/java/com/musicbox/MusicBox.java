package com.musicbox;

import com.mojang.logging.LogUtils;
import com.musicbox.gui.MusicBoxScreen;
import com.musicbox.music.MusicSettings;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Music Box ana sinifi.
 *
 * Mod sadece istemci tarafinda (oyuncunun bilgisayarinda) calisir: dist = Dist.CLIENT
 * oldugu icin sunucuda bu sinif hic yuklenmez.
 */
@Mod(value = MusicBox.MODID, dist = Dist.CLIENT)
public class MusicBox {
    public static final String MODID = "musicbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean autoDumpDone = false;

    public MusicBox(IEventBus modEventBus, ModContainer container) {
        MusicSettings.load();
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(MusicBoxCommands::register);
        NeoForge.EVENT_BUS.addListener(MusicBox::onClientTick);
        NeoForge.EVENT_BUS.addListener(MusicBox::onScreenInit);
        NeoForge.EVENT_BUS.addListener(MusicBox::addOptionsButton);

        // Mods listesinde Music Box > "Config" dugmesi paneli acar
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new MusicBoxScreen(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Music Box yuklendi.");
    }

    /**
     * Ayarlar (Options) ve Muzik ve Sesler (Music & Sounds) ekranlarinin sag ust kosesine
     * paneli acan bir "Music Box" dugmesi ekler.
     */
    private static void addOptionsButton(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof OptionsScreen) && !(screen instanceof SoundOptionsScreen)) return;
        int w = 90;
        event.addListener(Button.builder(Lang.tr("options.button"),
                        b -> Minecraft.getInstance().setScreen(new MusicBoxScreen(screen)))
                .bounds(screen.width - w - 6, 6, w, 20)
                .tooltip(Tooltip.create(Lang.tr("options.button.tooltip")))
                .build());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (guiTestTicks >= 0) guiTestTick(mc);
    }

    /**
     * Gelistirme testi (MUSICBOX_AUTODUMP=gui): paneli acar, ornek ayarlar ve bir onizlemeyle
     * ekran goruntusu alir, sonra ornek ayarlari geri alir.
     */
    private static int guiTestTicks = -1;

    private static void guiTestTick(Minecraft mc) {
        guiTestTicks++;
        var tracks = com.musicbox.music.MusicLibrary.scan().get("minecraft");
        switch (guiTestTicks) {
            case 100 -> {
                MusicSettings.setEnabled(tracks.get(2).id(), false);
                MusicSettings.setVolume(tracks.get(3).id(), 40);
                mc.setScreen(new MusicBoxScreen(new TitleScreen()));
            }
            case 105 -> com.musicbox.music.MusicPreview.play(tracks.get(1));
            case 125 -> shot(mc, "musicbox-panel.png");
            case 130 -> {
                MusicSettings.reset(tracks.get(2).id());
                MusicSettings.reset(tracks.get(3).id());
                mc.setScreen(new OptionsScreen(new TitleScreen(), mc.options)); // panel kapanir ve kaydeder
                guiTestTicks = -1;
            }
            default -> { }
        }
    }

    private static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                msg -> LOGGER.info("GUITEST ekran goruntusu: {}", msg.getString()));
    }

    /**
     * Gelistirme/test kolayligi: MUSICBOX_AUTODUMP ortam degiskeni ayarliysa, ana menu ilk acildiginda
     * (tum sesler yuklenmis olur) oz-testi calistirir ve muzik listesini musicbox-tracks.txt dosyasina yazar.
     * MUSICBOX_AUTODUMP=gui ise Ayarlar ekranlarinin ekran goruntulerini de alir.
     */
    private static void onScreenInit(ScreenEvent.Init.Post event) {
        String mode = System.getenv("MUSICBOX_AUTODUMP");
        if (autoDumpDone || !(event.getScreen() instanceof TitleScreen) || mode == null) return;
        autoDumpDone = true;
        try {
            DevSelfTest.run();
            LOGGER.info("MUSICBOX_AUTODUMP: liste yazildi -> {}", MusicBoxCommands.writeDump().toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("MUSICBOX_AUTODUMP basarisiz", e);
        }
        if (mode.equals("gui")) guiTestTicks = 0;
    }
}
