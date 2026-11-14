package com.quietplease.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quietplease.gui.GuiGraphics;
import com.quietplease.gui.SongPopup;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void quietplease$songPopup(PoseStack pose, float partialTick, CallbackInfo ci) {
        SongPopup.renderHud(new GuiGraphics(pose));
    }
}
