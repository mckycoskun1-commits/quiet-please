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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(QuietPlease.MODID)
public final class QuietPleaseForge {

    public QuietPleaseForge(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        QuietPlease.init();
        RegisterKeyMappingsEvent.BUS.addListener(event -> QuietPleaseKeys.all().forEach(event::register));
        RegisterClientCommandsEvent.BUS.addListener(event -> QuietPleaseCommands.register(event.getDispatcher()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> QuietPlease.clientTick());
        ScreenEvent.Init.Post.BUS.addListener(event -> {
            QuietPlease.onScreenInit(event.getScreen());
            MenuButton.onInit(event.getScreen(), event::addListener);
        });
        ScreenEvent.Render.Post.BUS.addListener(event -> MenuButton.onRender(event.getScreen(), event.getGuiGraphics()));
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new QuietPleaseScreen(parent)));
    }
}
