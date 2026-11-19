package com.quietplease.mixin;

//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.quietplease.gui.GuiGraphicsExtractor;
*///?}
import com.quietplease.gui.SongPopup;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
//? if <26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.gui.Hud;
*///?}
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(Hud.class)
*///?}
public abstract class GuiMixin {

    //? if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    //?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
    *///?}
    //? if >=1.21 {
    private void quietplease$songPopup(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
    //?} else if >=1.20 <1.21 {
    /*private void quietplease$songPopup(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
    *///?}
        //? if >=1.20 {
        SongPopup.renderHud(graphics);
        //?} else {
    /*private void quietplease$songPopup(PoseStack pose, float partialTick, CallbackInfo ci) {
        SongPopup.renderHud(new GuiGraphicsExtractor(pose));
        *///?}
    }
}
