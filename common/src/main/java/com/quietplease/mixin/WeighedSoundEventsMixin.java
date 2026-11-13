package com.quietplease.mixin;

import com.quietplease.music.MusicSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bir ses olayi (ornegin minecraft:music.game) calinacagi zaman oyun icindeki dosyalardan birini rastgele secer.
 * Burada kapatilan parcalari secimden cikariyoruz; boylece yerlerine hemen baska bir parca secilir.
 */
@Mixin(WeighedSoundEvents.class)
public abstract class WeighedSoundEventsMixin {

    @Shadow
    @Final
    private List<Weighted<Sound>> list;

    @org.spongepowered.asm.mixin.Unique
    private static final java.util.Random quietplease$random = new java.util.Random();

    @Inject(method = "getSound()Lnet/minecraft/client/resources/sounds/Sound;",
            at = @At("HEAD"), cancellable = true)
    private void quietplease$skipDisabledTracks(CallbackInfoReturnable<Sound> cir) {
        if (!MusicSettings.hasDisabled()) return;

        List<Weighted<Sound>> all = list;
        List<Weighted<Sound>> allowed = new ArrayList<>(all.size());
        boolean removedAny = false;
        int totalWeight = 0;
        for (Weighted<Sound> entry : all) {
            if (entry instanceof Sound sound && MusicSettings.isDisabled(sound.getLocation())) {
                removedAny = true;
            } else {
                allowed.add(entry);
                totalWeight += entry.getWeight();
            }
        }
        if (!removedAny) return; // bu olayda kapali parca yok, oyunun kendi secimi gecerli

        if (allowed.isEmpty() || totalWeight <= 0) {
            // Olaydaki tum parcalar kapali: "bilerek sessiz" sesi dondur, oyun hicbir sey calmaz.
            cir.setReturnValue(SoundManager.EMPTY_SOUND);
            return;
        }

        // Oyunun kendi agirlikli secimiyle ayni mantik, sadece izin verilen parcalar arasinda.
        int r = quietplease$random.nextInt(totalWeight);
        for (Weighted<Sound> entry : allowed) {
            r -= entry.getWeight();
            if (r < 0) {
                cir.setReturnValue(entry.getSound());
                return;
            }
        }
    }
}
