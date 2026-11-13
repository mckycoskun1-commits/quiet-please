package com.quietplease.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** 1.19.2 kaydiricisi: yeni surumlerdeki getX/getY/setPosition karsiliklari. */
public abstract class Slider extends AbstractSliderButton {

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
}
