package com.quietplease.gui;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** 1.19.2 dugmesi: yeni surumlerdeki Button.builder, setPosition ve setTooltip karsiliklari. */
public class Btn extends Button {

    @Nullable
    private List<FormattedCharSequence> tip;

    Btn(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, (b, pose, mouseX, mouseY) -> {
            List<FormattedCharSequence> t = ((Btn) b).tip;
            if (t != null && b.isMouseOver(mouseX, mouseY)) Tooltip.next(t);
        });
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public void setTooltip(@Nullable Tooltip tooltip) {
        this.tip = tooltip == null ? null : tooltip.lines;
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

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        @Nullable
        private Tooltip tooltip;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Btn build() {
            Btn b = new Btn(x, y, width, height, message, onPress);
            b.setTooltip(tooltip);
            return b;
        }
    }
}
