package com.quietplease.neoforge;

import com.quietplease.platform.Platform;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.sounds.Music;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.ClientHooks;

public final class NeoForgePlatform implements Platform {

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Optional<String> modName(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName());
    }

    @Override
    public Music selectMusic(Music situational) {
        return ClientHooks.selectMusic(situational, null);
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }
}
