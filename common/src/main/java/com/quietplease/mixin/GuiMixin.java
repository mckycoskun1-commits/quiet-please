package com.quietplease.mixin;

import com.quietplease.gui.SongPopup;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void quietplease$songPopup(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        SongPopup.renderHud(graphics);
    }
}
