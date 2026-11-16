package com.quietplease.mixin;

import com.quietplease.music.MusicMixer;
import com.quietplease.music.MusicSettings;
import java.util.List;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bir ses olayi (ornegin minecraft:music.game) calinacagi zaman oyun icindeki dosyalardan birini rastgele secer.
 * Kapali parcalar secimden cikar, son calinan parcalar (baska secenek varsa) atlanir, favoriler daha agir basar.
 *
 * Bu yontem oyundaki her seste (ayak sesi, blok kirma...) cagrilir. Cogunda hicbir sey degismez; o yuzden once
 * bellek ayirmadan bakilir, sadece gercekten degisen bir olayda secim yeniden yapilir.
 */
@Mixin(WeighedSoundEvents.class)
public abstract class WeighedSoundEventsMixin {

    @Shadow
    @Final
    private List<Weighted<Sound>> list;

    @Inject(method = "getSound(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/resources/sounds/Sound;",
            at = @At("HEAD"), cancellable = true)
    private void quietplease$pickTrack(RandomSource random, CallbackInfoReturnable<Sound> cir) {
        boolean disabled = MusicSettings.hasDisabled();
        boolean extras = MusicMixer.affectsSelection();
        if (!disabled && !extras) return;

        List<Weighted<Sound>> all = list;
        int n = all.size();
        if (!quietplease$anyAffected(all, n, disabled, extras)) return;

        // Agirlik -1: secimden cikti (kapali ya da az once caldi). rank: son calinanlar arasindaki sirasi, -1 = degil.
        int[] weights = new int[n];
        int[] rank = new int[n];
        int allowed = 0;
        for (int i = 0; i < n; i++) {
            Weighted<Sound> entry = all.get(i);
            int weight = entry.getWeight();
            rank[i] = -1;
            if (entry instanceof Sound sound) {
                ResourceLocation file = sound.getLocation();
                if (disabled && MusicSettings.isDisabled(file)) {
                    weights[i] = -1;
                    continue;
                }
                if (extras) {
                    weight = MusicMixer.weight(file, weight);
                    rank[i] = MusicMixer.recentRank(file);
                }
            }
            weights[i] = weight;
            allowed++;
        }

        // Tekrar etmeme: bu olayin son calinan parcalarindan en fazla yarisi disarida kalir. Boylece kucuk
        // havuzlarda (menu 9, Nether 5 sarki) secim hep ayni siraya donmez; en son calan parca ise (baska secenek
        // varsa) hic hemen tekrar gelmez.
        int skip = Math.min(com.quietplease.music.ModOptions.noRepeat, allowed > 1 ? Math.max(1, allowed / 2) : 0);
        for (int r = 0; skip > 0 && r < 10; r++) {
            for (int i = 0; i < n; i++) {
                if (rank[i] == r && weights[i] >= 0) {
                    weights[i] = -1;
                    skip--;
                    break;
                }
            }
        }

        int total = 0;
        for (int i = 0; i < n; i++) {
            if (weights[i] > 0) total += weights[i];
        }
        if (allowed == 0 || total <= 0) {
            cir.setReturnValue(SoundManager.EMPTY_SOUND);
            return;
        }

        int r = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            if (weights[i] <= 0) continue;
            r -= weights[i];
            if (r < 0) {
                cir.setReturnValue(all.get(i).getSound(random));
                return;
            }
        }
    }

    /** Bu olayda kapali, favori ya da son calinan bir dosya var mi? Yoksa oyunun kendi secimi aynen kalir. */
    private static boolean quietplease$anyAffected(List<Weighted<Sound>> all, int n, boolean disabled, boolean extras) {
        for (int i = 0; i < n; i++) {
            if (!(all.get(i) instanceof Sound sound)) continue;
            ResourceLocation file = sound.getLocation();
            if (disabled && MusicSettings.isDisabled(file)) return true;
            if (extras && (MusicMixer.isRecent(file) || MusicMixer.weight(file, 1) != 1)) return true;
        }
        return false;
    }
}
