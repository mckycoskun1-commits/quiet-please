package com.quietplease.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 1.19.2'de GuiGraphics yok; ekranlarin kullandigi kadarini PoseStack uzerinden saglayan kucuk bir sarmalayici.
 */
public final class GuiGraphics extends GuiComponent {

    private final PoseStack pose;

    public GuiGraphics(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return pose;
    }

    public int guiWidth() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(pose, x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        GuiComponent.fillGradient(pose, x1, y1, x2, y2, colorFrom, colorTo, 0);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawString(pose, font, text, x, y, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawString(pose, font, text, x, y, color);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        GuiComponent.drawString(pose, font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color);
        else font.draw(pose, text, x, y, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color);
        else font.draw(pose, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void drawWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            font.draw(pose, line, x, y, color);
            y += font.lineHeight;
        }
    }

    /** Esya cizimi PoseStack degil model-gorunum yiginini kullanir; o yuzden donusumu oraya aktariyoruz. */
    public void renderItem(ItemStack stack, int x, int y) {
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose();
        view.mulPoseMatrix(pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        Minecraft.getInstance().getItemRenderer().renderGuiItem(stack, x, y);
        view.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
