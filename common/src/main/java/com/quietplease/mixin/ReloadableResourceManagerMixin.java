package com.quietplease.mixin;

import com.quietplease.music.CustomMusic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Kendi muzik klasorundeki dosyalari oyunun kaynak yoneticisine tanitir (ses motoru, sure, ses dengeleme). */
@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void quietplease$customMusic(ResourceLocation id, CallbackInfoReturnable<Resource> cir) {
        if (CustomMusic.NAMESPACE.equals(id.getNamespace())) {
            Resource r = CustomMusic.resource(id);
            if (r != null) cir.setReturnValue(r);
        }
    }
}
