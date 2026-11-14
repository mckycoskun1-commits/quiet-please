package com.quietplease.forge;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import com.quietplease.gui.QuietPleaseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;

@Mod(QuietPlease.MODID)
public final class QuietPleaseForge {

    public QuietPleaseForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        // Sadece istemci modu: sunucuda olmamasi baglantiyi etkilemesin
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (version, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        QuietPlease.init();
        context.getModEventBus().addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> QuietPleaseKeys.all().forEach(ClientRegistry::registerKeyBinding)));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> QuietPleaseCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) QuietPlease.clientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.InitScreenEvent.Post event) -> {
            QuietPlease.onScreenInit(event.getScreen());
            MenuButton.onInit(event.getScreen(), event::addListener);
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) -> {
            if (event.getType() == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
                com.quietplease.gui.SongPopup.renderHud(new com.quietplease.gui.GuiGraphics(event.getMatrixStack()));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.DrawScreenEvent.Post event) ->
                MenuButton.onRender(event.getScreen(), event.getPoseStack(), event.getMouseX(), event.getMouseY()));
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new QuietPleaseScreen(parent)));
    }
}
