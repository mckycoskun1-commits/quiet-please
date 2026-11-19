package com.quietplease.platform;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.sounds.Music;

public interface Platform {

    Path configDir();

    Optional<String> modName(String modId);

    Music selectMusic(Music situational);

    boolean isDevelopment();
}
