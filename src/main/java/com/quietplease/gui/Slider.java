package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** 1.19.2 kaydiricisi: yeni surumlerdeki getX/getY/setPosition karsiliklari. */
public abstract class Slider extends AbstractSliderButton {

    @Nullable
    private List<FormattedCharSequence> tip;

    protected Slider(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setTooltip(@Nullable Tooltip tooltip) {
        this.tip = tooltip == null ? null : tooltip.lines;
    }

    @Override
    public void renderToolTip(PoseStack pose, int mouseX, int mouseY) {
        if (tip != null && isMouseOver(mouseX, mouseY)) Tooltip.next(tip);
    }
}
