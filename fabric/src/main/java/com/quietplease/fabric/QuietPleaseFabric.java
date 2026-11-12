package com.quietplease.fabric;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

public final class QuietPleaseFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        QuietPlease.init();
        QuietPleaseKeys.all().forEach(KeyMappingHelper::registerKeyMapping);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> QuietPleaseCommands.register(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(client -> QuietPlease.clientTick());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            QuietPlease.onScreenInit(screen);
            MenuButton.onInit(screen, button -> Screens.getWidgets(screen).add(button));
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> MenuButton.onRender(s, graphics));
        });
    }
}
