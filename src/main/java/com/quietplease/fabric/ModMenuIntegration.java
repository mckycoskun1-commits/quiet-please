package com.quietplease.fabric;

import com.quietplease.gui.QuietPleaseScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return QuietPleaseScreen::new;
    }
}
