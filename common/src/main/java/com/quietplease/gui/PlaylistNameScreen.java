package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import static com.quietplease.gui.QuietPleaseScreen.GOLD;
import static com.quietplease.gui.QuietPleaseScreen.MUTED;

import com.quietplease.Lang;
import com.quietplease.music.Playlist;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Yeni calma listesi ya da yeniden adlandirma: tek bir ad kutusu, Olustur / Iptal. */
public class PlaylistNameScreen extends Screen {

    private final Screen parent;
    @Nullable
    private final Playlist.SavedList renaming;
    private final Consumer<String> onDone;
    private String name;
    private HintBox box;
    private Btn done;

    public PlaylistNameScreen(Screen parent, @Nullable Playlist.SavedList renaming, Consumer<String> onDone) {
        super(Lang.tr(renaming == null ? "playlist.name.new" : "playlist.name.rename"));
        this.parent = parent;
        this.renaming = renaming;
        this.onDone = onDone;
        this.name = renaming == null ? "" : renaming.name();
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (width - w) / 2;
        int y = height / 2 - 20;
        box = new HintBox(font, x, y, w, 20, Lang.tr("playlist.name.hint"));
        box.setMaxLength(Playlist.NAME_MAX);
        box.setHint(Lang.tr("playlist.name.hint").withStyle(ChatFormatting.DARK_GRAY));
        box.setValue(name);
        box.setResponder(text -> {
            name = text;
            updateDone();
        });
        addRenderableWidget(box);
        setInitialFocus(box);
        done = addRenderableWidget(Btn.builder(Lang.tr(renaming == null ? "playlist.name.create" : "playlist.list.rename"),
                b -> finish()).bounds(width / 2 - 102, y + 40, 100, 20).build());
        addRenderableWidget(Btn.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 + 2, y + 40, 100, 20).build());
        updateDone();
    }

    private boolean valid() {
        return Playlist.isValidName(name, renaming);
    }

    private void updateDone() {
        if (done != null) done.active = valid();
    }

    private void finish() {
        if (!valid()) return;
        onDone.accept(name.trim());
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode;
        if ((key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)
                && box.isFocused()) {
            finish();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(pose);
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        Tooltip.renderPending(this, pose, mouseX, mouseY);
        int y = height / 2 - 20;
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, y - 18, GOLD);
        String trimmed = name.trim();
        if (!trimmed.isEmpty() && !valid()) {
            g.drawCenteredString(font, Lang.tr("playlist.name.taken"), width / 2, y + 26, 0xFFE07060);
        } else {
            g.drawCenteredString(font, com.quietplease.Txt.literal(trimmed.length() + " / " + Playlist.NAME_MAX), width / 2, y + 26, MUTED);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
