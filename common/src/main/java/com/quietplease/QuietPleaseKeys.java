package com.quietplease;

import com.mojang.blaze3d.platform.InputConstants;
import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.music.MusicControl;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Kisayol tuslari: Ayarlar → Kontroller → Tus Atamalari → "Quiet Please".
 *
 * Varsayilan olarak hicbir tusa atanmamis (baska modlarin tuslariyla cakismasin); oyuncu istedigi tusu secer.
 * Tuslar sadece oyundayken ve acik bir ekran yokken calisir (Minecraft'in kendi kurali).
 */
public final class QuietPleaseKeys {

    private static final String CATEGORY = "key.categories.quietplease";

    public static final KeyMapping OPEN_PANEL =
            new KeyMapping("key.quietplease.open", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping SKIP =
            new KeyMapping("key.quietplease.skip", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private QuietPleaseKeys() {}

    public static java.util.List<KeyMapping> all() {
        return java.util.List.of(OPEN_PANEL, SKIP);
    }

    /** Her istemci tick'inde: basilan tuslari isle. */
    public static void tick(Minecraft mc) {
        while (OPEN_PANEL.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new QuietPleaseScreen(null));
        }
        while (SKIP.consumeClick()) {
            String result = MusicControl.skipAndDescribe();
            // Sohbeti doldurmasin: kisa mesaj, hotbar'in ustunde birkac saniye
            mc.gui.setOverlayMessage(Lang.tr(result).withStyle(
                    result.equals("cmd.skip.nothing") ? ChatFormatting.GRAY : ChatFormatting.GREEN), false);
        }
    }
}
