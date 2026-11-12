package com.quietplease.forge;

import com.quietplease.platform.Platform;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.sounds.Music;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ForgePlatform implements Platform {

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
        return situational;
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }
}
