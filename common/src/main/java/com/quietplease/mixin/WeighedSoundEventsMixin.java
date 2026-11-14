package com.quietplease.mixin;

import com.quietplease.music.MusicMixer;
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
 * Kapali parcalar secimden cikar, son calinan parcalar (baska secenek varsa) atlanir, favoriler daha agir basar.
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
    private void quietplease$pickTrack(CallbackInfoReturnable<Sound> cir) {
        boolean disabled = MusicSettings.hasDisabled();
        boolean extras = MusicMixer.affectsSelection();
        if (!disabled && !extras) return;

        List<Weighted<Sound>> all = list;
        List<Weighted<Sound>> allowed = new ArrayList<>(all.size());
        List<Integer> weights = new ArrayList<>(all.size());
        List<Boolean> recent = new ArrayList<>(all.size());
        boolean changed = false;
        boolean anyFresh = false;
        for (Weighted<Sound> entry : all) {
            int weight = entry.getWeight();
            boolean isRecent = false;
            if (entry instanceof Sound sound) {
                if (disabled && MusicSettings.isDisabled(sound.getLocation())) {
                    changed = true;
                    continue;
                }
                if (extras) {
                    int w = MusicMixer.weight(sound.getLocation(), weight);
                    if (w != weight) changed = true;
                    weight = w;
                    isRecent = MusicMixer.isRecent(sound.getLocation());
                    if (isRecent) changed = true;
                }
            }
            if (!isRecent) anyFresh = true;
            allowed.add(entry);
            weights.add(weight);
            recent.add(isRecent);
        }
        if (!changed) return;

        int total = 0;
        for (int i = 0; i < allowed.size(); i++) {
            if (anyFresh && recent.get(i)) continue;
            total += weights.get(i);
        }
        if (allowed.isEmpty() || total <= 0) {
            cir.setReturnValue(SoundManager.EMPTY_SOUND);
            return;
        }

        int r = quietplease$random.nextInt(total);
        for (int i = 0; i < allowed.size(); i++) {
            if (anyFresh && recent.get(i)) continue;
            r -= weights.get(i);
            if (r < 0) {
                cir.setReturnValue(allowed.get(i).getSound());
                return;
            }
        }
    }
}
