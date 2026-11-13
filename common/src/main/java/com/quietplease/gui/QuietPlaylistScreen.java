package com.quietplease.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import static com.quietplease.gui.QuietPleaseScreen.ACCENT;
import static com.quietplease.gui.QuietPleaseScreen.ACCENT_SOFT;
import static com.quietplease.gui.QuietPleaseScreen.GOLD;
import static com.quietplease.gui.QuietPleaseScreen.HEADER_BG;
import static com.quietplease.gui.QuietPleaseScreen.MUTED;
import static com.quietplease.gui.QuietPleaseScreen.PANEL_BG;
import static com.quietplease.gui.QuietPleaseScreen.PLAYING;
import static com.quietplease.gui.QuietPleaseScreen.PLAYING_BG;
import static com.quietplease.gui.QuietPleaseScreen.ROW_ALT;
import static com.quietplease.gui.QuietPleaseScreen.ROW_HOVER;
import static com.quietplease.gui.QuietPleaseScreen.TEXT;

import com.quietplease.Lang;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.Playlist;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Calma listesi paneli: bir muzik calar gibi, en ustte calan sarki (ilerleme cubuguyla), altinda siradaki
 * sarkilar numarali. Sira degistirme (▲/▼), cikarma (✕), hemen calma (▶), atlama, karistirma, tekrar modu ve
 * "Sarki ekle" sekmesinde tum muziklerden ekleme.
 */
public class QuietPlaylistScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int BAR_Y = 54;
    private static final int BAR_H = 20;
    private static final int TOP = 80;
    private static final int BOTTOM_MARGIN = 46;
    private static final int MAX_ROW_WIDTH = 420;

    @Nullable
    private final Screen parent;

    private Map<String, List<MusicTrack>> library = Map.of();
    private Map<ResourceLocation, MusicTrack> tracksById = Map.of();
    private RowList list;
    @Nullable
    private CycleButton<String> modButton;
    private List<String> modValues = List.of();
    private Btn skip;
    private int shownVersion = -1;

    // Ekran yeniden boyutlaninca korunsun diye alanlarda
    private boolean addMode = false;
    private String query = "";
    private String modFilter = "";

    public QuietPlaylistScreen(@Nullable Screen parent) {
        super(Lang.tr("playlist.title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ kurulum

    private int rowW() {
        return Math.min(width - 20, MAX_ROW_WIDTH);
    }

    private int rowX() {
        return (width - rowW()) / 2;
    }

    @Override
    protected void init() {
        library = MusicLibrary.scan();
        tracksById = library.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(MusicTrack::id, t -> t, (a, b) -> a));
        int x = rowX();
        int rowW = rowW();

        // Sekmeler + calma listesi modu
        int y = 30;
        Btn queueTab = addRenderableWidget(Btn.builder(Lang.tr("playlist.tab.queue", Playlist.size()), b -> {
            addMode = false;
            init(minecraft, width, height);
        }).bounds(x, y, 90, 20).build());
        Btn addTab = addRenderableWidget(Btn.builder(Lang.tr("playlist.tab.add"), b -> {
            addMode = true;
            init(minecraft, width, height);
        }).bounds(x + 94, y, 90, 20).build());
        queueTab.active = addMode;
        addTab.active = !addMode;
        addRenderableWidget(CycleButton.onOffBuilder(ModOptions.playlistMode)
                .withTooltip(v -> Tooltip.lines(Lang.tr("settings.playlist_mode.tooltip")))
                .create(x + rowW - 150, y, 150, 20, Lang.tr("settings.playlist_mode"), (b, v) -> {
                    ModOptions.playlistMode = v;
                    MusicSettings.save();
                }));

        // Ikinci satir: sira sekmesinde "su an calan" cubugu + Atla; ekleme sekmesinde arama + mod filtresi
        y = BAR_Y;
        if (addMode) {
            int modW = 150;
            HintBox search = new HintBox(font, x + 1, y + 1, rowW - modW - 6, 18, Lang.tr("screen.search"));
            search.setHint(Lang.tr("screen.search").withStyle(ChatFormatting.DARK_GRAY));
            search.setValue(query);
            search.setResponder(text -> {
                query = text;
                rebuildList();
            });
            addRenderableWidget(search);
            List<String> mods = new ArrayList<>();
            mods.add("");
            mods.addAll(library.keySet());
            if (!mods.contains(modFilter)) modFilter = "";
            modValues = mods;
            modButton = addRenderableWidget(CycleButton.<String>builder(m -> m.isEmpty() ? Lang.tr("screen.mod.all") : com.quietplease.Txt.literal(MusicLibrary.modName(m)))
                    .withValues(mods)
                    .withInitialValue(modFilter)
                    .withTooltip(v -> Tooltip.lines(Lang.tr("screen.mod.tooltip")))
                    .create(x + rowW - modW, y, modW, 20, Lang.tr("screen.mod"), (b, value) -> {
                        modFilter = value;
                        rebuildList();
                    }));
        } else {
            modButton = null;
            Component skipLabel = Lang.tr("screen.skip");
            int skipW = Math.max(50, font.width(skipLabel) + 12);
            skip = addRenderableWidget(Btn.builder(skipLabel, b -> {
                        com.quietplease.music.MusicControl.skip();
                        rebuildList();
                    }).bounds(x + rowW - skipW, y, skipW, BAR_H)
                    .tooltip(Tooltip.create(Lang.tr("screen.skip.tooltip")))
                    .build());
        }

        list = new RowList(minecraft, width, height - TOP - BOTTOM_MARGIN, TOP);
        addRenderableWidget(list);
        rebuildList();

        // Alt satir
        int by = height - 28;
        if (addMode) {
            addRenderableWidget(Btn.builder(Lang.tr("playlist.back"), b -> {
                addMode = false;
                init(minecraft, width, height);
            }).bounds(width / 2 - 154, by, 150, 20).build());
            addRenderableWidget(Btn.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 + 4, by, 150, 20).build());
        } else {
            int bw = (rowW - 12) / 4;
            addRenderableWidget(Btn.builder(Lang.tr("playlist.shuffle"), b -> {
                        Playlist.shuffle();
                        rebuildList();
                    }).bounds(x, by, bw, 20)
                    .tooltip(Tooltip.create(Lang.tr("playlist.shuffle.tooltip")))
                    .build());
            addRenderableWidget(CycleButton.<Playlist.Repeat>builder(Playlist.Repeat::displayName)
                    .withValues(Playlist.Repeat.values())
                    .withInitialValue(Playlist.repeat())
                    .withTooltip(v -> Tooltip.lines(Lang.tr("playlist.repeat." + v.id + ".tooltip")))
                    .displayOnlyValue()
                    .create(x + bw + 4, by, bw, 20, Lang.tr("playlist.repeat"), (b, v) -> {
                        Playlist.setRepeat(v);
                        rebuildList();
                    }));
            addRenderableWidget(Btn.builder(Lang.tr("playlist.clear"), b -> minecraft.setScreen(new ConfirmScreen(ok -> {
                        if (ok) Playlist.clear();
                        minecraft.setScreen(this);
                    }, Lang.tr("playlist.clear.confirm"), Lang.tr("playlist.clear.confirm.message", Playlist.size()))))
                    .bounds(x + 2 * (bw + 4), by, bw, 20)
                    .build()).active = !Playlist.isEmpty();
            addRenderableWidget(Btn.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(x + 3 * (bw + 4), by, bw, 20).build());
        }
    }

    private void rebuildList() {
        if (list == null) return;
        double scroll = list.getScrollAmount();
        boolean sameView = shownVersion != -1;
        list.clear();
        if (addMode) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            library.forEach((mod, tracks) -> {
                if (!modFilter.isEmpty() && !modFilter.equals(mod)) return;
                String modName = MusicLibrary.modName(mod);
                List<MusicTrack> visible = tracks.stream()
                        .filter(t -> ModOptions.showAmbient || t.kind() != MusicTrack.Kind.OTHER || Playlist.contains(t.id()))
                        .filter(t -> q.isEmpty()
                                || t.id().toString().toLowerCase(Locale.ROOT).contains(q)
                                || modName.toLowerCase(Locale.ROOT).contains(q))
                        .toList();
                if (visible.isEmpty()) return;
                list.add(new ModHeader(modName, visible.size()));
                for (int i = 0; i < visible.size(); i++) list.add(new AddRow(visible.get(i), i % 2 == 1));
            });
        } else {
            List<Integer> order = Playlist.upcoming();
            for (int k = 0; k < order.size(); k++) list.add(new QueueRow(k, order));
        }
        shownVersion = Playlist.version();
        list.setScrollAmount(sameView ? scroll : 0);
    }

    // ------------------------------------------------------------------ cizim

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(pose);
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        renderHeader(g);
        if (!addMode) renderNowPlaying(g);
        if (list.children().isEmpty()) {
            Component msg = addMode ? Lang.tr("screen.no_results") : Lang.tr("playlist.empty");
            g.drawCenteredString(font, msg, width / 2, TOP + 20, MUTED);
            if (!addMode) g.drawCenteredString(font, Lang.tr("playlist.empty.hint"), width / 2, TOP + 34, MUTED);
        } else if (!addMode && ModOptions.gap == ModOptions.Gap.VANILLA) {
            // Ipucu: vanilla sarki arasi oyunda 5-20 dk
            g.drawCenteredString(font, Lang.tr("playlist.gap_hint"), width / 2, height - 40, MUTED);
        }
        Tooltip.renderPending(this, pose, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g) {
        Component title = Lang.tr("playlist.title").withStyle(ChatFormatting.BOLD);
        MutableComponent summary = Lang.count("tracks", Playlist.size()).append(" · ")
                .append(Lang.tr(ModOptions.playlistMode ? "playlist.mode_on" : "playlist.mode_off"));
        int logo = 22;
        int textW = Math.max(font.width(title), font.width(summary));
        int x = (width - (logo + 6 + textW)) / 2;
        int y = 4;
        QuietPleaseScreen.drawLogo(g, x, y, logo);
        g.drawString(font, title, x + logo + 6, y + 2, GOLD);
        g.drawString(font, summary, x + logo + 6, y + 13, ModOptions.playlistMode ? PLAYING : MUTED);
    }

    /** En ustte: calma listesinden calan sarki, ilerleme cubugu ve sure. */
    private void renderNowPlaying(GuiGraphics g) {
        int left = rowX();
        int right = skip.getX() - 4;
        int top = BAR_Y;
        Playlist.PlaylistSound playing = Playlist.playing();
        g.fill(left, top, right, top + BAR_H, PLAYING_BG);
        g.fill(left, top, left + 2, top + BAR_H, playing != null ? PLAYING : MUTED);
        skip.active = minecraft.getMusicManager() != null
                && ((com.quietplease.mixin.MusicManagerAccessor) minecraft.getMusicManager()).quietplease$getCurrentMusic() != null;

        if (playing == null) {
            Component line;
            if (Playlist.isEmpty()) line = Lang.tr("playlist.bar.empty");
            else if (!ModOptions.playlistMode) line = Lang.tr("playlist.bar.off");
            else if (Playlist.hasEnded()) line = Lang.tr("playlist.bar.ended");
            else {
                List<Integer> order = Playlist.upcoming();
                ResourceLocation next = order.isEmpty() ? Playlist.tracks().get(0) : Playlist.tracks().get(order.get(0));
                line = Lang.tr("playlist.bar.next", com.quietplease.Txt.literal(shortName(next)).withStyle(s -> s.withColor(TEXT & 0xFFFFFF)));
            }
            g.drawString(font, fit(line, right - left - 12), left + 8, top + 6, MUTED);
            return;
        }

        QuietPleaseScreen.drawEqualizer(g, left + 7, top + 12, PLAYING);
        int elapsed = Playlist.elapsedSeconds();
        int total = Playlist.durationSeconds(playing);
        String time = formatTime(elapsed) + (total > 0 ? " / " + formatTime(total) : "");
        int timeW = font.width(time);
        MutableComponent line = com.quietplease.Txt.literal(shortName(playing.trackId)).withStyle(s -> s.withColor(PLAYING & 0xFFFFFF))
                .append(com.quietplease.Txt.literal("  " + MusicLibrary.modName(playing.trackId.getNamespace())).withStyle(ChatFormatting.GRAY));
        g.drawString(font, fit(line, right - left - 30 - timeW - 8), left + 20, top + 3, TEXT);
        g.drawString(font, time, right - 4 - timeW, top + 3, MUTED);
        // ilerleme cubugu
        int barL = left + 20;
        int barR = right - 4;
        g.fill(barL, top + 14, barR, top + 16, 0x40FFFFFF);
        if (total > 0) {
            int fill = (int) ((barR - barL) * Math.min(1.0, elapsed / (double) total));
            g.fill(barL, top + 14, barL + fill, top + 16, PLAYING);
        }
    }

    private Component fit(Component text, int maxW) {
        if (font.width(text) <= maxW) return text;
        return com.quietplease.Txt.literal(font.plainSubstrByWidth(text.getString(), Math.max(0, maxW - font.width("…"))) + "…")
                .withStyle(text.getStyle());
    }

    /** 75 -> "1:15" */
    private static String formatTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String shortName(ResourceLocation id) {
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public void tick() {
        // Sarki degisince (ya da baska yerden liste degisince) sirayi yenile
        if (shownVersion != Playlist.version()) {
            if (addMode) shownVersion = Playlist.version();
            else rebuildList();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Mod filtresine sag tik: bir onceki mod (ana paneldeki gibi)
        if (button == 1 && modButton != null && modButton.isMouseOver(mouseX, mouseY) && !modValues.isEmpty()) {
            int i = modValues.indexOf(modButton.getValue());
            String previous = modValues.get(Math.floorMod(i - 1, modValues.size()));
            modButton.setValue(previous);
            modFilter = previous;
            rebuildList();
            modButton.playDownSound(minecraft.getSoundManager());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        MusicSettings.save();
    }

    // ------------------------------------------------------------------ liste

    private class RowList extends ContainerObjectSelectionList<Row> {
        /** 1.19.2 listeleri kendi alanina kirpmaz; tasan satirlar alttaki dugmelerin ustune cizilmesin. */
        @Override
        protected void renderList(PoseStack pose, int x, int y, int mouseX, int mouseY, float partialTick) {
            GuiGraphics.enableScissor(x0, y0, x1, y1);
            super.renderList(pose, x, y, mouseX, mouseY, partialTick);
            GuiGraphics.disableScissor();
        }

        RowList(Minecraft mc, int width, int height, int y) {
            super(mc, width, y + height, y, y + height, ROW_HEIGHT);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void clear() {
            clearEntries();
        }

        void add(Row row) {
            addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return Math.min(width - 40, MAX_ROW_WIDTH);
        }

        @Override
        protected int getScrollbarPosition() {
            return getRowLeft() + getRowWidth() + 8;
        }

        @Override
        protected void renderBackground(PoseStack pose) {
            GuiGraphics g = new GuiGraphics(pose);
            int left = getRowLeft() - 6;
            int right = getScrollbarPosition() + 6 + 4;
            int top = y0 - 2;
            int bottom = y1 + 2;
            g.fill(left, top, right, bottom, PANEL_BG);
            g.renderOutline(left - 1, top - 1, right - left + 2, bottom - top + 2, ACCENT_SOFT);
            g.fillGradient(left, top, right, top + 6, 0x30E89A3C, 0x00E89A3C);
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {}

    private static Btn iconButton(String symbol, Component tooltip, Runnable action) {
        return Btn.builder(com.quietplease.Txt.literal(symbol), b -> action.run()).size(18, 18).tooltip(Tooltip.create(tooltip)).build();
    }

    /** Siradaki bir sarki: numara, ad, mod, [▶] [▲] [▼] [✕]. */
    private class QueueRow extends Row {
        private final int number;
        private final int index;
        private final boolean nextRound;
        private final Btn play;
        private final Btn up;
        private final Btn down;
        private final Btn remove;
        private final List<Btn> buttons;

        QueueRow(int k, List<Integer> order) {
            this.number = k + 1;
            this.index = order.get(k);
            // "Listeyi tekrarla" modunda bir sonraki tur da normal gorunur; digerlerinde soluk
            this.nextRound = Playlist.isNextRound(index) && Playlist.repeat() != Playlist.Repeat.REPEAT_LIST;
            this.play = iconButton("▶", Lang.tr("playlist.play_now.tooltip"), () -> {
                Playlist.playNow(index);
                rebuildList();
            });
            this.up = iconButton("▲", Lang.tr("playlist.up.tooltip"), () -> {
                Playlist.swap(index, order.get(k - 1));
                rebuildList();
            });
            this.down = iconButton("▼", Lang.tr("playlist.down.tooltip"), () -> {
                Playlist.swap(index, order.get(k + 1));
                rebuildList();
            });
            this.remove = iconButton("✕", Lang.tr("playlist.remove.tooltip"), () -> {
                Playlist.removeAt(index);
                rebuildList();
            });
            up.active = k > 0;
            down.active = k < order.size() - 1;
            this.buttons = List.of(play, up, down, remove);
        }

        @Override
        public void render(PoseStack pose, int i, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            GuiGraphics g = new GuiGraphics(pose);
            if (index >= Playlist.size()) return; // liste degisti, yenilenmeyi bekliyor
            ResourceLocation id = Playlist.tracks().get(index);
            MusicTrack track = tracksById.get(id);
            boolean disabled = MusicSettings.isDisabled(id);
            boolean missing = track == null;
            play.active = !disabled && !missing;

            if (hovering) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_HOVER);
            else if (number % 2 == 0) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_ALT);
            if (number == 1 && !nextRound) g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT); // siradaki

            int textY = top + (height - 8) / 2;
            String num = number + ".";
            g.drawString(font, num, left + 18 - font.width(num), textY, MUTED);
            int nameX = left + 24;
            int buttonsX = left + width - 4 * 20 + 2;
            int x = buttonsX;
            for (Btn b : buttons) {
                b.setPosition(x, top + (height - 18) / 2);
                x += 20;
            }

            int color = nextRound ? MUTED : TEXT;
            MutableComponent name = com.quietplease.Txt.literal(shortName(id));
            if (disabled) name.withStyle(ChatFormatting.STRIKETHROUGH);
            MutableComponent info = com.quietplease.Txt.literal("  " + MusicLibrary.modName(id.getNamespace()));
            if (missing) info = com.quietplease.Txt.literal("  ").append(Lang.tr("playlist.missing")).withStyle(ChatFormatting.RED);
            else if (disabled) info.append(" · ").append(Lang.tr("screen.off"));
            MutableComponent line = name.withStyle(s -> s.withColor((disabled ? 0xFF6A6058 : color) & 0xFFFFFF))
                    .append(info.withStyle(missing ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
            g.drawString(font, fit(line, buttonsX - 6 - nameX), nameX, textY, color);

            for (Btn b : buttons) b.render(g.pose(), mouseX, mouseY, partialTick);
            if (hovering && nextRound && mouseX < buttonsX) Tooltip.next(Lang.tr("playlist.next_round.tooltip"));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }
    }

    /** "Sarki ekle" sekmesinde mod basligi. */
    private class ModHeader extends Row {
        private final String modName;
        private final int count;

        ModHeader(String modName, int count) {
            this.modName = modName;
            this.count = count;
        }

        @Override
        public void render(PoseStack pose, int i, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            GuiGraphics g = new GuiGraphics(pose);
            g.fill(left - 3, top - 1, left + width + 3, top + height + 1, HEADER_BG);
            g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            Component name = com.quietplease.Txt.literal(modName).withStyle(ChatFormatting.BOLD);
            int y = top + (height - 8) / 2;
            g.drawString(font, name, left + 4, y, GOLD);
            g.drawString(font, Lang.count("tracks", count), left + 10 + font.width(name), y, MUTED);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    /** "Sarki ekle" sekmesinde bir parca: ikon, ad, [+ Ekle] / [✓ Listede]. */
    private class AddRow extends Row {
        private final MusicTrack track;
        private final boolean alternate;
        private final ItemStack icon;
        private final Btn toggle;

        AddRow(MusicTrack track, boolean alternate) {
            this.track = track;
            this.alternate = alternate;
            this.icon = QuietPleaseScreen.iconFor(track);
            this.toggle = Btn.builder(com.quietplease.Txt.empty(), b -> Playlist.toggle(track.id()))
                    .size(70, 18).build();
        }

        @Override
        public void render(PoseStack pose, int i, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            GuiGraphics g = new GuiGraphics(pose);
            boolean in = Playlist.contains(track.id());
            if (hovering) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_HOVER);
            else if (alternate) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_ALT);
            g.renderItem(icon, left + 2, top + (height - 16) / 2);
            toggle.setPosition(left + width - toggle.getWidth(), top + (height - 18) / 2);
            toggle.setMessage(in ? Lang.tr("playlist.added").withStyle(ChatFormatting.GREEN) : Lang.tr("playlist.add"));
            toggle.setTooltip(Tooltip.create(Lang.tr(in ? "playlist.added.tooltip" : "playlist.add.tooltip")));
            boolean disabled = MusicSettings.isDisabled(track.id());
            MutableComponent name = com.quietplease.Txt.literal(track.shortName());
            if (disabled) name.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY);
            MutableComponent line = name.append(com.quietplease.Txt.literal("  ").append(track.kind().displayName()).withStyle(ChatFormatting.DARK_GRAY));
            g.drawString(font, fit(line, toggle.getX() - 6 - (left + 22)), left + 22, top + (height - 8) / 2, in ? GOLD : TEXT);
            toggle.render(g.pose(), mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(toggle);
        }
    }
}
