package com.quietplease.forge;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import com.quietplease.gui.QuietPleaseScreen;
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.19 {
import net.minecraftforge.client.ConfigScreenHandler;
//?} else {
/*import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
*///?}
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
//? if >=1.19 {
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
//?}
import net.minecraftforge.client.event.ScreenEvent;
//? if <26.1 {
/*import net.minecraftforge.common.MinecraftForge;
*///?}
import net.minecraftforge.event.TickEvent;
//? if <1.19 {
/*import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
*///?}
import net.minecraftforge.fml.common.Mod;
//? if <1.19 {
/*import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
*///?}
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
//? if <1.19 {
/*import net.minecraftforge.network.NetworkConstants;
*///?}

@Mod(QuietPlease.MODID)
public final class QuietPleaseForge {

    //? if >=1.21 {
    public QuietPleaseForge(FMLJavaModLoadingContext context) {
    //?} else {
    /*public QuietPleaseForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
    *///?}
        //? if <1.19 {
        /*// Sadece istemci modu: sunucuda olmamasi baglantiyi etkilemesin
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (version, fromServer) -> true));
        *///?}
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        QuietPlease.init();
        //? if >=26.1 {
        RegisterKeyMappingsEvent.BUS.addListener(event -> QuietPleaseKeys.all().forEach(event::register));
        RegisterClientCommandsEvent.BUS.addListener(event -> QuietPleaseCommands.register(event.getDispatcher()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> QuietPlease.clientTick());
        ScreenEvent.Init.Post.BUS.addListener(event -> {
        //?} else if >=1.19 <26.1 {
        /*context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> QuietPleaseKeys.all().forEach(event::register));
        *///?} else {
        /*context.getModEventBus().addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> QuietPleaseKeys.all().forEach(ClientRegistry::registerKeyBinding)));
        *///?}
        //? if <26.1 {
        /*MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> QuietPleaseCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) QuietPlease.clientTick();
        });
        *///?}
        //? if >=1.19 <26.1 {
        /*MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
        *///?} else if <1.19 {
        /*MinecraftForge.EVENT_BUS.addListener((ScreenEvent.InitScreenEvent.Post event) -> {
        *///?}
            QuietPlease.onScreenInit(event.getScreen());
            MenuButton.onInit(event.getScreen(), event::addListener);
        });
        //? if >=26.1 {
        ScreenEvent.Render.Post.BUS.addListener(event -> MenuButton.onRender(event.getScreen(), event.getGuiGraphics()));
        //?} else if >=1.20 <1.21 {
        /*MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiEvent.Post event) -> com.quietplease.gui.SongPopup.renderHud(event.getGuiGraphics()));
        *///?}
        //? if >=1.20 <26.1 {
        /*MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> MenuButton.onRender(event.getScreen(), event.getGuiGraphics()));
        *///?} else if >=1.19 <1.20 {
        /*MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiEvent.Post event) -> com.quietplease.gui.SongPopup.renderHud(new com.quietplease.gui.GuiGraphicsExtractor(event.getPoseStack())));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> MenuButton.onRender(event.getScreen(), event.getPoseStack(), event.getMouseX(), event.getMouseY()));
        *///?}
        //? if >=1.19 {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new QuietPleaseScreen(parent)));
        //?} else {
        /*MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) -> {
            if (event.getType() == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
                com.quietplease.gui.SongPopup.renderHud(new com.quietplease.gui.GuiGraphicsExtractor(event.getMatrixStack()));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.DrawScreenEvent.Post event) ->
                MenuButton.onRender(event.getScreen(), event.getPoseStack(), event.getMouseX(), event.getMouseY()));
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new QuietPleaseScreen(parent)));
        *///?}
    }
}
