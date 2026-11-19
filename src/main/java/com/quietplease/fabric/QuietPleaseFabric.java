package com.quietplease.fabric;

import com.quietplease.QuietPlease;
import com.quietplease.QuietPleaseCommands;
import com.quietplease.QuietPleaseKeys;
import com.quietplease.gui.MenuButton;
import net.fabricmc.api.ClientModInitializer;
//? if >=1.19 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//?} else {
/*import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
*///?}
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

public final class QuietPleaseFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        QuietPlease.init();
        //? if >=26.1 {
        QuietPleaseKeys.all().forEach(KeyMappingHelper::registerKeyMapping);
        //?} else {
        /*QuietPleaseKeys.all().forEach(KeyBindingHelper::registerKeyBinding);
        *///?}
        //? if >=1.19 {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> QuietPleaseCommands.register(dispatcher));
        //?} else {
        /*QuietPleaseCommands.register(ClientCommandManager.DISPATCHER);
        *///?}
        ClientTickEvents.END_CLIENT_TICK.register(client -> QuietPlease.clientTick());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            QuietPlease.onScreenInit(screen);
            //? if >=26.1 {
            MenuButton.onInit(screen, button -> Screens.getWidgets(screen).add(button));
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> MenuButton.onRender(s, graphics));
            //?} else {
            /*MenuButton.onInit(screen, button -> Screens.getButtons(screen).add(button));
            *///?}
            //? if >=1.20 <26.1 {
            /*ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> MenuButton.onRender(s, graphics));
            *///?} else if <1.20 {
            /*ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> MenuButton.onRender(s, graphics, mouseX, mouseY));
            *///?}
        });
    }
}
