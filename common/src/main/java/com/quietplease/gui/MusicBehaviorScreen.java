package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicMixer;
import com.quietplease.music.MusicSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class MusicBehaviorScreen extends Screen {

    private static final int BUTTON_W = 150;

    @Nullable
    private final Screen parent;

    public MusicBehaviorScreen(@Nullable Screen parent) {
        super(Lang.tr("behavior.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_W - 4;
        int right = width / 2 + 4;
        int row = 24;
        int y = 36;

        addRenderableWidget(choice(left, y, "behavior.jukebox", ModOptions.JUKEBOX_VALUES, ModOptions.jukeboxVolume,
                MusicBehaviorScreen::volumeName, v -> ModOptions.jukeboxVolume = v));
        addRenderableWidget(choice(right, y, "behavior.unfocused", ModOptions.UNFOCUSED_VALUES, ModOptions.unfocusedVolume,
                MusicBehaviorScreen::volumeName, v -> ModOptions.unfocusedVolume = v));
        y += row;

        addRenderableWidget(choice(left, y, "behavior.combat", ModOptions.COMBAT_VALUES, ModOptions.combatVolume,
                v -> v >= 100 ? Lang.tr("behavior.off") : Lang.tr("behavior.combat." + v), v -> ModOptions.combatVolume = v));
        addRenderableWidget(choice(right, y, "behavior.fade", ModOptions.FADE_VALUES, ModOptions.fadeSeconds,
                v -> v <= 0 ? Lang.tr("behavior.off") : Lang.tr("behavior.seconds", v), v -> ModOptions.fadeSeconds = v));
        y += row;

        addRenderableWidget(choice(left, y, "behavior.no_repeat", ModOptions.NO_REPEAT_VALUES, ModOptions.noRepeat,
                v -> v <= 0 ? Lang.tr("behavior.off") : Lang.tr("behavior.last", v), v -> {
                    ModOptions.noRepeat = v;
                    MusicMixer.optionsChanged();
                }));
        addRenderableWidget(choice(right, y, "behavior.favorites", ModOptions.FAVORITE_VALUES, ModOptions.favoriteWeight,
                v -> v <= 1 ? Lang.tr("behavior.off") : Lang.tr("behavior.times", v), v -> ModOptions.favoriteWeight = v));
        y += row;

        addRenderableWidget(CycleButton.onOffBuilder(ModOptions.fadeOnSkip)
                .withTooltip(v -> Tooltip.create(Lang.tr("settings.fade_skip.tooltip")))
                .create(left, y, BUTTON_W, 20, Lang.tr("settings.fade_skip"), (b, v) -> ModOptions.fadeOnSkip = v));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private static Component volumeName(int v) {
        if (v >= 100) return Lang.tr("behavior.off");
        if (v <= 0) return Lang.tr("behavior.mute");
        return Lang.tr("behavior.lower", v);
    }

    private static CycleButton<Integer> choice(int x, int y, String key, int[] values, int current,
            IntFunction<Component> name, IntConsumer onChange) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int v : values) list.add(v);
        return CycleButton.<Integer>builder(name::apply)
                .withInitialValue(current)
                .withValues(list)
                .withTooltip(v -> Tooltip.create(Lang.tr(key + ".tooltip")))
                .create(x, y, BUTTON_W, 20, Lang.tr(key), (b, v) -> onChange.accept(v));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFC966);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        MusicSettings.save();
    }
}
