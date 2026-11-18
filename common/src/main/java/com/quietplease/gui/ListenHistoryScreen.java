package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.music.ListenStats;
import com.quietplease.music.MusicControl;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.NowPlaying;
import com.quietplease.music.Playlist;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Dinleme gecmisi ve istatistikleri: son calanlar, en cok calanlar ve hic calmayan sarkilar.
 * Her satirda: simdi cal, favori ve acik/kapali dugmeleri.
 */
public class ListenHistoryScreen extends Screen {

    public enum Tab {
        RECENT("history.tab.recent"),
        TOP("history.tab.top"),
        NEVER("history.tab.never");

        final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private record Row(ResourceLocation id, @Nullable MusicTrack track, Component info, int historyIndex) {}

    private static final int ROW_H = 22;
    private static final int MAX_W = 460;
    private static final int LIST_TOP = 66;

    @Nullable
    private final Screen parent;
    private Tab tab;
    private int offset;
    private int seenVersion = -1;
    @Nullable
    private Map<ResourceLocation, MusicTrack> tracksById;
    private List<Row> rows = List.of();
    private int visibleRows;
    private int left;
    private int rowW;

    public ListenHistoryScreen(@Nullable Screen parent, Tab tab) {
        super(Lang.tr("history.title"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        if (tracksById == null) {
            tracksById = MusicLibrary.scan().values().stream().flatMap(List::stream)
                    .collect(Collectors.toMap(MusicTrack::id, t -> t, (a, b) -> a));
        }
        seenVersion = ListenStats.version();
        rows = buildRows();
        rowW = Math.min(width - 20, MAX_W);
        left = (width - rowW) / 2;
        visibleRows = Math.max(1, (height - 36 - LIST_TOP) / ROW_H);
        offset = Math.max(0, Math.min(offset, rows.size() - visibleRows));

        int tabW = (rowW - 8) / 3;
        int x = left;
        for (Tab t : Tab.values()) {
            Btn b = addRenderableWidget(Btn.builder(Lang.tr(t.key), btn -> {
                        tab = t;
                        offset = 0;
                        rebuildWidgets();
                    }).bounds(x, 30, tabW, 20)
                    .tooltip(Tooltip.create(Lang.tr(t.key + ".tooltip")))
                    .build());
            b.active = t != tab;
            x += tabW + 4;
        }

        for (int i = 0; i < visibleRows && offset + i < rows.size(); i++) {
            addRowButtons(rows.get(offset + i), LIST_TOP + i * ROW_H);
        }

        int by = height - 28;
        addRenderableWidget(Btn.builder(Lang.tr("history.clear"), b -> minecraft.setScreen(new ConfirmScreen(yes -> {
                    if (yes) ListenStats.clear();
                    minecraft.setScreen(this);
                }, Lang.tr("history.clear"), Lang.tr("history.clear.confirm"))))
                .bounds(width / 2 - 154, by, 150, 20)
                .tooltip(Tooltip.create(Lang.tr("history.clear.tooltip")))
                .build());
        addRenderableWidget(Btn.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, by, 150, 20).build());
    }

    private void addRowButtons(Row row, int y) {
        ResourceLocation id = row.id();
        boolean enabled = !MusicSettings.isDisabled(id);
        int right = left + rowW;
        int toggleW = 54;
        Btn toggle = Btn.builder(Lang.tr(enabled ? "screen.on" : "screen.off")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), b -> {
                    MusicSettings.setEnabled(id, !MusicSettings.get(id).enabled());
                    MusicSettings.save();
                    MusicSettings.applyToPlayingSounds();
                    rebuildWidgets();
                }).bounds(right - toggleW, y + 1, toggleW, 20)
                .tooltip(Tooltip.create(Lang.tr("history.toggle.tooltip")))
                .build();
        addRenderableWidget(toggle);

        boolean fav = Playlist.isFavorite(id);
        addRenderableWidget(Btn.builder(Component.literal(fav ? "★" : "☆")
                        .withStyle(fav ? ChatFormatting.GOLD : ChatFormatting.GRAY), b -> {
                    Playlist.toggleFavorite(id);
                    MusicSettings.save();
                    rebuildWidgets();
                }).bounds(right - toggleW - 24, y + 1, 20, 20)
                .tooltip(Tooltip.create(Lang.tr(fav ? "screen.star.remove" : "screen.star.add")))
                .build());

        Btn play = addRenderableWidget(Btn.builder(Component.literal("▶"), b -> {
                    if (row.historyIndex() >= 0) MusicControl.setBackIndex(row.historyIndex());
                    MusicControl.playFromHistory(id, row.historyIndex() >= 0);
                }).bounds(right - toggleW - 48, y + 1, 20, 20)
                .tooltip(Tooltip.create(Lang.tr("history.play.tooltip")))
                .build());
        play.active = enabled && row.track() != null && row.track().kind().isSong();
    }

    private List<Row> buildRows() {
        List<Row> out = new ArrayList<>();
        switch (tab) {
            case RECENT -> {
                List<ListenStats.Entry> history = ListenStats.history();
                long now = System.currentTimeMillis();
                for (int i = 0; i < history.size(); i++) {
                    ListenStats.Entry e = history.get(i);
                    out.add(new Row(e.id(), tracksById.get(e.id()), ago(now - e.time()), i));
                }
            }
            case TOP -> {
                for (Map.Entry<ResourceLocation, ListenStats.Plays> e : ListenStats.mostPlayed()) {
                    out.add(new Row(e.getKey(), tracksById.get(e.getKey()),
                            Lang.count("plays", e.getValue().count()), -1));
                }
            }
            case NEVER -> {
                tracksById.values().stream()
                        .filter(t -> t.kind() == MusicTrack.Kind.MUSIC)
                        .filter(t -> ListenStats.plays(t.id()).count() == 0)
                        .sorted(Comparator.comparing((MusicTrack t) -> !t.namespace().equals("minecraft"))
                                .thenComparing(MusicTrack::namespace).thenComparing(t -> t.id().getPath()))
                        .forEach(t -> out.add(new Row(t.id(), t, Component.empty(), -1)));
            }
        }
        return out;
    }

    private static Component ago(long ms) {
        long min = ms / 60_000;
        if (min < 1) return Lang.tr("history.ago.now");
        if (min < 60) return Lang.tr("history.ago.min", min);
        long hours = min / 60;
        if (hours < 24) return Lang.tr("history.ago.hour", hours);
        return Lang.tr("history.ago.day", hours / 24);
    }

    @Override
    public void tick() {
        // Yeni bir sarki baslayinca liste guncellensin
        if (ListenStats.version() != seenVersion) rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int max = Math.max(0, rows.size() - visibleRows);
        int next = Math.max(0, Math.min(max, offset - (int) Math.signum(scrollY) * 3));
        if (next != offset) {
            offset = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(pose);
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFC966);
        if (rows.isEmpty()) {
            g.drawCenteredString(font, Lang.tr(tab == Tab.NEVER ? "history.never.empty" : "history.empty"),
                    width / 2, LIST_TOP + 20, 0xFFA0A0A0);
            Tooltip.renderPending(this, pose, mouseX, mouseY);
            return;
        }
        List<ResourceLocation> playing = tracksById == null ? List.of()
                : NowPlaying.find(tracksById).stream().map(MusicTrack::id).toList();
        int textRight = left + rowW - 54 - 48 - 6;
        for (int i = 0; i < visibleRows && offset + i < rows.size(); i++) {
            Row row = rows.get(offset + i);
            int y = LIST_TOP + i * ROW_H;
            if (i % 2 == 1) g.fill(left, y, left + rowW, y + ROW_H, 0x20FFFFFF);
            if (row.track() != null) g.renderItem(QuietPleaseScreen.iconFor(row.track()), left + 2, y + 3);
            String name = com.quietplease.music.MusicTrack.title(row.id());
            boolean now = playing.contains(row.id());
            boolean off = MusicSettings.isDisabled(row.id());
            int nameColor = now ? 0xFF7CFC7C : off ? 0xFF808080 : 0xFFFFFFFF;
            String infoText = row.info().getString();
            int infoW = infoText.isEmpty() ? 0 : font.width(infoText) + 8;
            int textX = left + 22;
            String shown = font.plainSubstrByWidth((now ? "♪ " : "") + name, textRight - textX - infoW);
            g.drawString(font, shown, textX, y + 2, nameColor, false);
            String mod = font.plainSubstrByWidth(MusicLibrary.modName(row.id().getNamespace()), textRight - textX - infoW);
            g.drawString(font, mod, textX, y + 12, 0xFF9A8C78, false);
            if (!infoText.isEmpty()) g.drawString(font, infoText, textRight - font.width(infoText), y + 7, 0xFFC0C0C0, false);
        }
        if (rows.size() > visibleRows) {
            String page = (offset + 1) + "-" + Math.min(rows.size(), offset + visibleRows) + " / " + rows.size();
            g.drawString(font, page, left + rowW - font.width(page), LIST_TOP - 11, 0xFF808080, false);
        }
        Tooltip.renderPending(this, pose, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
