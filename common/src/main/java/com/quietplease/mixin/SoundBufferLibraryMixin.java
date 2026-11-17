package com.quietplease.mixin;

import com.quietplease.music.CustomMusic;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ses motoru dosyalari oyunun kaynak yoneticisinden degil, yuklemede toplanan bir haritadan okur
 * (ResourceProvider.fromMap). Kendi muzik klasorundeki dosyalar o haritada yoktur; saglayiciyi sarip onlari da ekler.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

    @Shadow
    @Final
    @Mutable
    private ResourceProvider resourceManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void quietplease$customMusic(ResourceProvider provider, CallbackInfo ci) {
        this.resourceManager = CustomMusic.provider(this.resourceManager);
    }
}
