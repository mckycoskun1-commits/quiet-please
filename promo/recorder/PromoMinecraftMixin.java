package com.quietplease.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PromoMinecraftMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void quietplease$promo(boolean advance, CallbackInfo ci) {
        com.quietplease.PromoDirector.frame();
    }
}
