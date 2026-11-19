package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** 1.19.2'de EditBox.setHint yok: kutu bosken soluk bir ipucu yazisi cizen arama kutusu. */
public class HintBox extends EditBox {

    private final Font font;
    @Nullable
    private Component hint;

    public HintBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
    }

    public void setHint(Component hint) {
        this.hint = hint;
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.renderButton(pose, mouseX, mouseY, partialTick);
        if (hint != null && isVisible() && getValue().isEmpty() && !isFocused()) {
            font.drawShadow(pose, hint, x + 4, y + (height - 8) / 2, 0xFF555555);
        }
    }
}
