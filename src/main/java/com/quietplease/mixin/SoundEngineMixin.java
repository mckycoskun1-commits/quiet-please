package com.quietplease.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.quietplease.music.MusicSettings;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ses motorunun ses seviyesi hesabini parca ayariyla carpar.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    /** Bir ses ilk calmaya baslarken. Sonuc 0 olursa oyun sesi hic baslatmaz. */
    @ModifyExpressionValue(method = "play",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float quietplease$volumeOnPlay(float original, @Local(argsOnly = true) SoundInstance sound) {
        return original * MusicSettings.factor(sound);
    }

    /**
     * Calmakta olan seslerin seviyesi yeniden hesaplanirken (ayar degisince ya da ses kaydiricisi oynatilinca).
     * Sonuc 0 olursa oyun o sesi durdurur.
     */
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void quietplease$volumeWhilePlaying(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float factor = MusicSettings.factor(sound);
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValueF() * factor);
    }
}
