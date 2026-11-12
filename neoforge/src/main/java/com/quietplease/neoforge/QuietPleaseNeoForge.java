package com.quietplease.neoforge;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import com.quietplease.gui.QuietPleaseScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = QuietPlease.MODID, dist = Dist.CLIENT)
public final class QuietPleaseNeoForge {

    public QuietPleaseNeoForge(IEventBus modBus, ModContainer container) {
        QuietPlease.init();
        modBus.addListener((RegisterKeyMappingsEvent event) -> QuietPleaseKeys.all().forEach(event::register));
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> QuietPleaseCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> QuietPlease.clientTick());
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            QuietPlease.onScreenInit(event.getScreen());
            MenuButton.onInit(event.getScreen(), event::addListener);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> MenuButton.onRender(event.getScreen(), event.getGuiGraphics()));
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new QuietPleaseScreen(parent));
    }
}
