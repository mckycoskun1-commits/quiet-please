package com.quietplease.mixin;

import com.quietplease.music.MusicSettings;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ses motorunun ses seviyesi hesabini parca ayariyla carpar.
 * 1.18.2'de play() da calculateVolume(SoundInstance) cagirir; tek enjeksiyon hem baslangici hem calan sesleri kapsar.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    /**
     * Calmakta olan seslerin seviyesi yeniden hesaplanirken (ayar degisince ya da ses kaydiricisi oynatilinca).
     * Sonuc 0 olursa oyun o sesi durdurur.
     */
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void quietplease$volumeWhilePlaying(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float factor = MusicSettings.factor(sound) * com.quietplease.music.MusicControl.fadeFactor(sound);
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValueF() * factor);
    }
}
