package com.quietplease.mixin;

import com.quietplease.music.CustomMusic;
//? if >=1.19 {
import java.util.Optional;
//?}
import net.minecraft.resources.Identifier;
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
    //? if >=1.19 {
    private void quietplease$customMusic(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
    //?} else {
    /*private void quietplease$customMusic(Identifier id, CallbackInfoReturnable<Resource> cir) {
    *///?}
        if (CustomMusic.NAMESPACE.equals(id.getNamespace())) {
            //? if >=1.19 {
            cir.setReturnValue(CustomMusic.resource(id, (ReloadableResourceManager) (Object) this));
            //?} else {
            /*Resource r = CustomMusic.resource(id);
            if (r != null) cir.setReturnValue(r);
            *///?}
        }
    }
}
