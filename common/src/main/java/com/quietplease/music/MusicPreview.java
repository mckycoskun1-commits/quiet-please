package com.quietplease.music;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * Panelde bir parcayi dinleyerek onizleme.
 *
 * Onizleme calarken oyunun arka plan muzigi susturulur (bkz. {@link MusicSettings#factor(SoundInstance)}),
 * boylece iki muzik ust uste binmez.
 */
public final class MusicPreview {

    /** Rastgele secim yapmadan, dogrudan belirli bir dosyayi calan ses. */
    public static final class PreviewSound extends AbstractSoundInstance {
        private final Sound target;
        final ResourceLocation trackId;

        PreviewSound(ResourceLocation eventId, Sound target, ResourceLocation trackId) {
            // MASTER kanalindan calar: oyuncunun "Muzik" kaydiricisi %0 olsa bile onizleme duyulur
            // (MUSIC kanalindan calinca oyun sesi hic baslatmiyordu).
            super(eventId, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.target = target;
            this.trackId = trackId;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = target; // oyunun rastgele secimini atla
            return manager.getSoundEvent(this.location);
        }
    }

    @Nullable
    private static PreviewSound current;

    private MusicPreview() {}

    /** Parcayi calar. Baska bir onizleme caliyorsa once onu durdurur. */
    public static boolean play(MusicTrack track) {
        stop();
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation eventId : track.events()) {
            WeighedSoundEvents event = sm.getSoundEvent(eventId);
            if (event == null) continue;
            for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) event).quietplease$getList()) {
                if (w instanceof Sound s && s.getLocation().equals(track.id())) {
                    current = new PreviewSound(eventId, s, track.id());
                    MusicSettings.applyToPlayingSounds(); // calan arka plan muzigini sustur
                    sm.play(current);
                    return true;
                }
            }
        }
        return false;
    }

    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    /**
     * Su an bir onizleme caliyor mu? Parca kendiliginden bittiyse durumu da temizler.
     * Oyun duraklatilmisken Minecraft biten sesleri temizlemedigi icin ses kanalinin durumuna da bakilir.
     */
    public static boolean isActive() {
        if (current == null) return false;
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        ChannelAccess.ChannelHandle channel = ((com.quietplease.mixin.SoundEngineAccessor) ((com.quietplease.mixin.SoundManagerAccessor) sm).quietplease$getSoundEngine()).quietplease$getInstanceToChannel().get(current);
        boolean finished = !sm.isActive(current) || (channel != null && channel.isStopped());
        if (finished) stop();
        return current != null;
    }

    public static boolean isPlaying(ResourceLocation trackId) {
        return isActive() && current.trackId.equals(trackId);
    }

    /** Ses motorunun icinden cagrilir: durum degistirmez, sadece okur. */
    public static boolean hasPreview() {
        return current != null;
    }
}
