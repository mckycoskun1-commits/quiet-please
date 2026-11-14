package com.quietplease.forge;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import com.quietplease.gui.QuietPleaseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(QuietPlease.MODID)
public final class QuietPleaseForge {

    public QuietPleaseForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        QuietPlease.init();
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> QuietPleaseKeys.all().forEach(event::register));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> QuietPleaseCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) QuietPlease.clientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            QuietPlease.onScreenInit(event.getScreen());
            MenuButton.onInit(event.getScreen(), event::addListener);
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiEvent.Post event) -> com.quietplease.gui.SongPopup.renderHud(event.getGuiGraphics()));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> MenuButton.onRender(event.getScreen(), event.getGuiGraphics()));
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new QuietPleaseScreen(parent)));
    }
}
