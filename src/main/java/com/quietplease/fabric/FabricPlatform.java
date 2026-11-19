package com.quietplease.fabric;

import com.quietplease.platform.Platform;
import java.nio.file.Path;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sounds.Music;

public final class FabricPlatform implements Platform {

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Optional<String> modName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName());
    }

    @Override
    public Music selectMusic(Music situational) {
        return situational;
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
