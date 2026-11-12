package com.quietplease.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicPreview;
import com.quietplease.music.Playlist;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arka plan muzigi: sarki arasi (bekleme suresi) ve calma listesi.
 *
 * tick() her tick'te o anki "durum muzigini" (Music) secer; iki sarki arasindaki bekleme bu nesnenin
 * getMinDelay()/getMaxDelay() degerlerinden gelir. Arka plan muzigiyse ayara gore farkli sureli bir kopyasiyla
 * degistiriyoruz. Ejderha/jenerik ve modlarin "mevcut muzigi kes" isaretli muzikleri degismez.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Shadow
    private int nextSongDelay;

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private Music quietplease$adjustGap(Music music) {
        return ModOptions.adjustGap(music);
    }

    /**
     * Vanilla, calan sarki yeni durum muziginden farkliysa ve yeni muzik "kes" isaretliyse (menu, End, ejderha)
     * calani keser. Calan bir calma listesi sarkisiysa ve yeni muzik de bir arka plan muzigiyse kesmesin;
     * yoksa ornegin ana menude her liste sarkisi hemen kesilirdi. Ejderha/jenerik yine keser.
     */
    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;replaceCurrentMusic()Z"))
    private boolean quietplease$keepPlaylistSong(boolean replace, @Local(ordinal = 0) Music music) {
        return replace && !(currentMusic instanceof Playlist.PlaylistSound && ModOptions.isBackground(music));
    }

    /** Arka plan muzigi baslayacakken: calma listesi aciksa oyunun rastgele secimi yerine listedeki siradaki parca. */
    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void quietplease$playlist(Music selector, CallbackInfo ci) {
        if (!Playlist.wouldTakeOver(selector)) return;
        if (MusicPreview.hasPreview()) {
            // Onizleme calarken arka plan muzigi sessizdir; liste bosuna ilerlemesin, 1 sn sonra tekrar dene.
            currentMusic = null;
            nextSongDelay = 20;
            ci.cancel();
            return;
        }
        SoundInstance sound = Playlist.startInstead(selector);
        if (sound == null) return; // calinabilecek parca yok: oyun kendi sectigini calsin
        currentMusic = sound;
        minecraft.getSoundManager().play(sound);
        nextSongDelay = Integer.MAX_VALUE;
        ci.cancel();
    }
}
