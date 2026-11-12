package com.quietplease;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Ceviri yardimcisi. Tum metinler assets/quietplease/lang/*.json dosyalarinda durur
 * (en_us varsayilan, tr_tr Turkce); oyun hangi dildeyse o dosya kullanilir.
 */
public final class Lang {

    private Lang() {}

    /** "quietplease.&lt;key&gt;" cevirisi. */
    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(QuietPlease.MODID + "." + key, args);
    }

    /** Sayili ifade, Ingilizce tekil/cogul farkiyla: count("tracks", 1) -> "1 track", count("tracks", 5) -> "5 tracks". */
    public static MutableComponent count(String what, long n) {
        return tr("count." + what + (n == 1 ? ".one" : ".many"), n);
    }
}
