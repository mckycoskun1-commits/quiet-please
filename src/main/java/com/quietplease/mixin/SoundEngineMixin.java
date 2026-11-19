package com.quietplease.mixin;

//? if >=1.19 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
//?}
import com.quietplease.music.MusicSettings;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.19 {
/**
 * Ses motorunun ses seviyesi hesabini parca ayariyla carpar.
 */
@Mixin(SoundEngine.class)
//?} else {
/*/^*
 * Ses motorunun ses seviyesi hesabini parca ayariyla carpar.
 * 1.18.2'de play() da calculateVolume(SoundInstance) cagirir; tek enjeksiyon hem baslangici hem calan sesleri kapsar.
 ^/
@Mixin(SoundEngine.class)
*///?}
public abstract class SoundEngineMixin {

    //? if >=1.19 <26.3 {
    /** Bir ses ilk calmaya baslarken. Sonuc 0 olursa oyun sesi hic baslatmaz. */
    @ModifyExpressionValue(method = "play",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float quietplease$volumeOnPlay(float original, @Local(argsOnly = true) SoundInstance sound) {
        return original * MusicSettings.factor(sound) * com.quietplease.music.MusicControl.fadeFactor(sound)
                * com.quietplease.music.MusicMixer.factor(sound) * com.quietplease.music.FocusMute.factor();
    }

    //?} else if >=26.3 {
    /*/^* Bir ses ilk calmaya baslarken. Sonuc 0 olursa oyun sesi hic baslatmaz. ^/
    @ModifyExpressionValue(method = {"play", "playWithoutEvent"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float quietplease$volumeOnPlay(float original, @Local(argsOnly = true) SoundInstance sound) {
        return original * MusicSettings.factor(sound) * com.quietplease.music.MusicControl.fadeFactor(sound)
                * com.quietplease.music.MusicMixer.factor(sound) * com.quietplease.music.FocusMute.factor();
    }

    *///?}
    /**
     * Calmakta olan seslerin seviyesi yeniden hesaplanirken (ayar degisince ya da ses kaydiricisi oynatilinca).
     * Sonuc 0 olursa oyun o sesi durdurur.
     */
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void quietplease$volumeWhilePlaying(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float factor = MusicSettings.factor(sound) * com.quietplease.music.MusicControl.fadeFactor(sound)
                * com.quietplease.music.MusicMixer.factor(sound) * com.quietplease.music.FocusMute.factor();
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValueF() * factor);
    }

    /** Oyun duraklatma menusunden donunce butun sesleri devam ettirir; oyuncunun duraklattigi sarki duraklatilmis kalsin. */
    @Inject(method = "resume", at = @At("TAIL"))
    private void quietplease$keepPaused(CallbackInfo ci) {
        com.quietplease.music.MusicControl.keepPaused();
    }
}
