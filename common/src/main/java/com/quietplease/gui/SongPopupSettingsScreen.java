package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicSettings;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class SongPopupSettingsScreen extends Screen {

    private static final int BUTTON_W = 150;

    @Nullable
    private final Screen parent;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public SongPopupSettingsScreen(@Nullable Screen parent) {
        super(Lang.tr("popup.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_W - 4;
        int right = width / 2 + 4;
        int row = height < 250 ? 22 : 24;
        int y = height < 250 ? 30 : 34;

        addRenderableWidget(CycleButton.<ModOptions.PopupPosition>builder(ModOptions.PopupPosition::displayName, ModOptions.popupPosition)
                .withValues(ModOptions.PopupPosition.values())
                .withTooltip(v -> Tooltip.create(Lang.tr("settings.popup_position.tooltip")))
                .create(left, y, BUTTON_W, 20, Lang.tr("popup.position"), (b, v) -> {
                    ModOptions.popupPosition = v;
                    SongPopup.replayPreview();
                }));
        addRenderableWidget(cycle(ModOptions.PopupBackground.values(), ModOptions.popupBackground, ModOptions.PopupBackground::displayName,
                right, y, "popup.background", v -> ModOptions.popupBackground = v));
        y += row;

        addRenderableWidget(new IntSlider(left, y, ModOptions.POPUP_SCALE_MIN, ModOptions.POPUP_SCALE_MAX, 5, ModOptions.popupScale,
                v -> Lang.tr("popup.scale", v + "%"), v -> ModOptions.popupScale = v));
        addRenderableWidget(new IntSlider(right, y, ModOptions.POPUP_DURATION_MIN, ModOptions.POPUP_DURATION_MAX, 1, ModOptions.popupDuration,
                v -> Lang.tr("popup.duration", v + " s"), v -> ModOptions.popupDuration = v))
                .setTooltip(Tooltip.create(Lang.tr("popup.duration.tooltip")));
        y += row;

        addRenderableWidget(new IntSlider(left, y, 0, ModOptions.POPUP_OFFSET_X_MAX, 2, ModOptions.popupOffsetX,
                v -> Lang.tr("popup.offset_x", v), v -> ModOptions.popupOffsetX = v))
                .setTooltip(Tooltip.create(Lang.tr("popup.offset_x.tooltip")));
        addRenderableWidget(new IntSlider(right, y, -ModOptions.POPUP_OFFSET_Y_MAX, ModOptions.POPUP_OFFSET_Y_MAX, 2, ModOptions.popupOffsetY,
                v -> Lang.tr("popup.offset_y", v > 0 ? "+" + v : String.valueOf(v)), v -> ModOptions.popupOffsetY = v))
                .setTooltip(Tooltip.create(Lang.tr("popup.offset_y.tooltip")));
        y += row;

        addRenderableWidget(cycle(ModOptions.PopupColor.values(), ModOptions.popupTitleColor, ModOptions.PopupColor::displayName,
                left, y, "popup.title_color", v -> ModOptions.popupTitleColor = v));
        addRenderableWidget(cycle(ModOptions.PopupColor.values(), ModOptions.popupModColor, ModOptions.PopupColor::displayName,
                right, y, "popup.mod_color", v -> ModOptions.popupModColor = v));
        y += row;

        addRenderableWidget(cycle(ModOptions.TextStyle.values(), ModOptions.popupTextStyle, ModOptions.TextStyle::displayName,
                left, y, "popup.style", v -> ModOptions.popupTextStyle = v));
        addRenderableWidget(toggle(right, y, "popup.shadow", ModOptions.popupShadow, v -> ModOptions.popupShadow = v));
        y += row;

        addRenderableWidget(toggle(left, y, "popup.show_icon", ModOptions.popupShowIcon, v -> ModOptions.popupShowIcon = v));
        addRenderableWidget(toggle(right, y, "popup.show_mod", ModOptions.popupShowMod, v -> ModOptions.popupShowMod = v));
        y += row;

        addRenderableWidget(toggle(left, y, "popup.equalizer", ModOptions.popupEqualizer, v -> ModOptions.popupEqualizer = v));
        addRenderableWidget(Button.builder(Lang.tr("popup.replay"), b -> SongPopup.replayPreview())
                .bounds(right, y, BUTTON_W, 20)
                .tooltip(Tooltip.create(Lang.tr("popup.replay.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Lang.tr("popup.reset"), b -> {
                    ModOptions.resetPopup();
                    SongPopup.replayPreview();
                    rebuildWidgets();
                })
                .bounds(left, height - 28, BUTTON_W, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(right, height - 28, BUTTON_W, 20).build());
    }

    private static <T> CycleButton<T> cycle(T[] values, T value, java.util.function.Function<T, Component> name,
            int x, int y, String key, Consumer<T> onChange) {
        return CycleButton.<T>builder(name, value)
                .withValues(values)
                .create(x, y, BUTTON_W, 20, Lang.tr(key), (b, v) -> onChange.accept(v));
    }

    private static CycleButton<Boolean> toggle(int x, int y, String key, boolean value, Consumer<Boolean> onChange) {
        return CycleButton.onOffBuilder(value).create(x, y, BUTTON_W, 20, Lang.tr(key), (b, v) -> onChange.accept(v));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 12, 0xFFFFC966);
        g.centeredText(font, Lang.tr("popup.drag_hint"), width / 2, height - 42, 0xFF9A8C78);
        SongPopup.renderPreview(g, dragging || overPreview(mouseX, mouseY));
    }

    private boolean overPreview(double mouseX, double mouseY) {
        int[] b = SongPopup.previewBounds(width, height);
        return mouseX >= b[0] - 2 && mouseX < b[0] + b[2] + 2 && mouseY >= b[1] - 2 && mouseY < b[1] + b[3] + 2;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && overPreview(event.x(), event.y())) {
            int[] b = SongPopup.previewBounds(width, height);
            dragging = true;
            grabX = event.x() - b[0];
            grabY = event.y() - b[1];
            SongPopup.finishPreviewAnimation();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            SongPopup.placeAt((int) Math.round(event.x() - grabX), (int) Math.round(event.y() - grabY), width, height);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            rebuildWidgets();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        MusicSettings.save();
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final int step;
        private final IntFunction<Component> label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int min, int max, int step, int value, IntFunction<Component> label, IntConsumer onChange) {
            super(x, y, BUTTON_W, 20, Component.empty(), (value - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        int current() {
            int v = min + (int) Math.round(value * (max - min) / step) * step;
            return Mth.clamp(v, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(current()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}
