package com.quietplease.mixin;

import com.quietplease.music.ModOptions;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Arka plan muzigi: sarki arasi (bekleme suresi).
 *
 * tick() her tick'te o anki "durum muzigini" (Music) secer; iki sarki arasindaki bekleme bu nesnenin
 * getMinDelay()/getMaxDelay() degerlerinden gelir. Arka plan muzigiyse ayara gore farkli sureli bir kopyasiyla
 * degistiriyoruz. Ejderha/jenerik ve modlarin "mevcut muzigi kes" isaretli muzikleri degismez.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private Music quietplease$adjustGap(Music music) {
        return ModOptions.adjustGap(music);
    }
}
