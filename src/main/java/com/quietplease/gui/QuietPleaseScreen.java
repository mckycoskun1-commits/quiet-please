package com.quietplease.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.quietplease.QuietPlease;
import com.quietplease.Lang;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicPreview;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.NowPlaying;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quiet Please paneli: tum muzikler modlara gore gruplanmis halde, her parca icin
 * dinleme (onizleme), ses kaydiricisi ve acik/kapali dugmesi.
 */
public class QuietPleaseScreen extends Screen {

    // ------------------------------------------------------------------ gorunum ayarlari

    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath(QuietPlease.MODID, "textures/gui/logo.png");

    /** Ikondaki sicak turuncu isiltiyla uyumlu vurgu rengi. */
    private static final int ACCENT = 0xFFE89A3C;
    private static final int ACCENT_SOFT = 0x80E89A3C;
    private static final int PANEL_BG = 0xB0100C08;
    private static final int HEADER_BG = 0x803A2210;
    private static final int ROW_ALT = 0x10FFFFFF;
    private static final int ROW_HOVER = 0x24FFD9A0;
    private static final int GOLD = 0xFFFFC966;
    private static final int TEXT = 0xFFF2E6C9;
    private static final int MUTED = 0xFF9A8C78;

    private static final int PLAYING = 0xFF7EE08A;      // oyunda su an calan parca (yesil)
    private static final int PLAYING_BG = 0x2470D080;

    private static final int ROW_HEIGHT = 24;
    private static final int NOW_PLAYING_Y = 56;
    private static final int NOW_PLAYING_H = 16;
    private static final int TOP = 78;
    private static final int BOTTOM_MARGIN = 36;
    private static final int MAX_ROW_WIDTH = 420;

    @Nullable
    private final Screen parent;

    private Map<String, List<MusicTrack>> library = Map.of();
    private List<MusicTrack> allTracks = List.of();
    private Map<ResourceLocation, MusicTrack> tracksById = Map.of();
    /** Oyunda su an calan parcalar (her tick'te guncellenir). */
    private List<MusicTrack> nowPlaying = List.of();
    private int nowPlayingIndex = 0;
    private TrackList list;

    // Filtreler (ekran yeniden boyutlaninca korunsun diye alanlarda tutuluyor)
    private String query = "";
    private String modFilter = ""; // "" = tum modlar
    private boolean showOther = false;

    public QuietPleaseScreen(@Nullable Screen parent) {
        super(Component.literal("Quiet Please"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ kurulum

    @Override
    protected void init() {
        library = MusicLibrary.scan();
        allTracks = library.values().stream().flatMap(List::stream).toList();
        tracksById = allTracks.stream().collect(Collectors.toMap(MusicTrack::id, t -> t, (a, b) -> a));
        nowPlaying = NowPlaying.find(tracksById);

        // Ust satir: arama | mod filtresi | ortam sesleri
        int rowW = Math.min(width - 20, MAX_ROW_WIDTH);
        int x = (width - rowW) / 2;
        int modW = 150;
        int otherW = 96;
        int searchW = rowW - modW - otherW - 8;
        int y = 32;

        EditBox search = new EditBox(font, x + 1, y + 1, searchW - 2, 18, Lang.tr("screen.search"));
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
        addRenderableWidget(CycleButton.<String>builder(m -> m.isEmpty() ? Lang.tr("screen.mod.all") : Component.literal(MusicLibrary.modName(m)))
                .withValues(mods)
                .withInitialValue(modFilter)
                .create(x + searchW + 4, y, modW, 20, Lang.tr("screen.mod"), (b, value) -> {
                    modFilter = value;
                    rebuildList();
                }));

        addRenderableWidget(CycleButton.booleanBuilder(
                        Lang.tr("screen.ambient.show").withStyle(ChatFormatting.GREEN),
                        Lang.tr("screen.ambient.hide").withStyle(ChatFormatting.GRAY))
                .withInitialValue(showOther)
                .withTooltip(v -> Tooltip.create(Lang.tr("screen.ambient.tooltip")))
                .create(x + searchW + 4 + modW + 4, y, otherW, 20, Lang.tr("screen.ambient"), (b, value) -> {
                    showOther = value;
                    rebuildList();
                }));

        // Liste
        list = new TrackList(minecraft, width, height - TOP - BOTTOM_MARGIN, TOP);
        addRenderableWidget(list);
        rebuildList();

        // Alt satir
        int by = height - 28;
        addRenderableWidget(Button.builder(Lang.tr("screen.reset_all"), b -> {
                    MusicPreview.stop();
                    MusicSettings.resetAll();
                    MusicSettings.applyToPlayingSounds();
                    rebuildWidgets();
                }).bounds(width / 2 - 154, by, 150, 20)
                .tooltip(Tooltip.create(Lang.tr("screen.reset_all.tooltip")))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, by, 150, 20).build());
    }

    private void rebuildList() {
        if (list == null) return;
        String q = query.trim().toLowerCase(Locale.ROOT);
        list.clear();
        library.forEach((mod, tracks) -> {
            if (!modFilter.isEmpty() && !modFilter.equals(mod)) return;
            String modName = MusicLibrary.modName(mod);
            List<MusicTrack> visible = tracks.stream()
                    .filter(t -> showOther || t.kind() != MusicTrack.Kind.OTHER)
                    .filter(t -> q.isEmpty()
                            || t.id().toString().toLowerCase(Locale.ROOT).contains(q)
                            || modName.toLowerCase(Locale.ROOT).contains(q))
                    .collect(Collectors.toList());
            if (visible.isEmpty()) return;
            list.add(new HeaderEntry(modName, visible));
            for (int i = 0; i < visible.size(); i++) list.add(new TrackEntry(visible.get(i), i % 2 == 1));
        });
        list.setScrollAmount(0);
    }

    // ------------------------------------------------------------------ cizim ve yasam dongusu

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderHeader(g);
        renderNowPlaying(g, mouseX, mouseY);
        if (list.children().isEmpty()) {
            g.drawCenteredString(font, Lang.tr("screen.no_results"), width / 2, TOP + 24, MUTED);
        }
    }

    /** Logo + baslik + ozet satiri. */
    private void renderHeader(GuiGraphics g) {
        Component title = Component.literal("Quiet Please").withStyle(ChatFormatting.BOLD);
        long off = allTracks.stream().filter(t -> MusicSettings.isDisabled(t.id())).count();
        long custom = allTracks.stream().filter(t -> !MusicSettings.isDisabled(t.id())
                && MusicSettings.get(t.id()).volume() != MusicSettings.DEFAULT_VOLUME).count();
        MutableComponent summary = Lang.count("sources", library.size()).append(" · ").append(Lang.count("tracks", allTracks.size()));
        if (off > 0) summary.append(" · ").append(Lang.tr("count.off", off));
        if (custom > 0) summary.append(" · ").append(Lang.tr("count.adjusted", custom));

        int logo = 22;
        int textW = Math.max(font.width(title), font.width(summary));
        int x = (width - (logo + 6 + textW)) / 2;
        int y = 5;

        // Logo: ince turuncu cerceveli kare
        g.fill(x - 1, y - 1, x + logo + 1, y + logo + 1, ACCENT_SOFT);
        RenderSystem.enableBlend();
        g.blit(LOGO, x, y, 0, 0, logo, logo, logo, logo);
        RenderSystem.disableBlend();

        g.drawString(font, title, x + logo + 6, y + 2, GOLD);
        g.drawString(font, summary, x + logo + 6, y + 13, MUTED);
    }

    // ------------------------------------------------------------------ "su an calan" cubugu

    private int barLeft() {
        return list.getRowLeft() - 6;
    }

    private int barRight() {
        return list.getScrollbarPosition() + 10;
    }

    private boolean overNowPlaying(double mouseX, double mouseY) {
        return mouseX >= barLeft() && mouseX < barRight()
                && mouseY >= NOW_PLAYING_Y && mouseY < NOW_PLAYING_Y + NOW_PLAYING_H;
    }

    /** Listenin ustunde: oyunda su an hangi parca caliyor (oyuncu muzigin adini bilmese de bulabilsin diye). */
    private void renderNowPlaying(GuiGraphics g, int mouseX, int mouseY) {
        int left = barLeft();
        int right = barRight();
        int top = NOW_PLAYING_Y;
        boolean hover = !nowPlaying.isEmpty() && overNowPlaying(mouseX, mouseY);
        g.fill(left, top, right, top + NOW_PLAYING_H, hover ? 0x5070D080 : PLAYING_BG);
        g.fill(left, top, left + 2, top + NOW_PLAYING_H, nowPlaying.isEmpty() ? MUTED : PLAYING);
        int textY = top + (NOW_PLAYING_H - 8) / 2;

        if (nowPlaying.isEmpty()) {
            g.drawString(font, Lang.tr("screen.now_playing.none"), left + 8, textY, MUTED);
            return;
        }
        MusicTrack shown = nowPlaying.get(Math.floorMod(nowPlayingIndex, nowPlaying.size()));
        drawEqualizer(g, left + 7, top + NOW_PLAYING_H - 4, PLAYING);
        MutableComponent line = Lang.tr("screen.now_playing").append(" ")
                .append(Component.literal(shown.shortName()).withStyle(s -> s.withColor(PLAYING & 0xFFFFFF)))
                .append(Component.literal("  " + MusicLibrary.modName(shown.namespace())).withStyle(ChatFormatting.GRAY));
        if (nowPlaying.size() > 1) line.append(Component.literal("  ")).append(Lang.tr("screen.now_playing.more", nowPlaying.size() - 1).withStyle(ChatFormatting.DARK_GRAY));
        g.drawString(font, line, left + 20, textY, TEXT);
        if (hover) setTooltipForNextRenderPass(Lang.tr("screen.now_playing.tooltip"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !nowPlaying.isEmpty() && overNowPlaying(mouseX, mouseY)) {
            // Tiklaninca calan parcayi listede goster; birden fazla parca caliyorsa her tiklamada siradakine gec.
            MusicTrack shown = nowPlaying.get(Math.floorMod(nowPlayingIndex, nowPlaying.size()));
            nowPlayingIndex++;
            query = shown.shortName();
            modFilter = "";
            if (shown.kind() == MusicTrack.Kind.OTHER) showOther = true;
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        MusicPreview.isActive(); // biten onizlemeyi temizler, boylece arka plan muzigi geri gelebilir
        nowPlaying = NowPlaying.find(tracksById);
    }

    /**
     * Panel acikken tek oyunculu oyun duraklar. Onizleme yine de calar: Minecraft duraklarken sadece o an
     * calan sesleri bekletir, yeni baslatilan sesler calar (bkz. MusicPreview).
     */
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
        MusicPreview.stop();
        MusicSettings.save();
    }

    // ------------------------------------------------------------------ liste

    private class TrackList extends ContainerObjectSelectionList<Row> {
        TrackList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, ROW_HEIGHT);
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

        /** Oyunun dokulu liste zemini yerine koyu, turuncu cerceveli bir panel. */
        @Override
        protected void renderListBackground(GuiGraphics g) {
            int left = getRowLeft() - 6;
            int right = getScrollbarPosition() + 6 + 4;
            int top = getY() - 2;
            int bottom = getBottom() + 2;
            g.fill(left, top, right, bottom, PANEL_BG);
            g.renderOutline(left - 1, top - 1, right - left + 2, bottom - top + 2, ACCENT_SOFT);
            // ust kenarda hafif bir isilti
            g.fillGradient(left, top, right, top + 6, 0x30E89A3C, 0x00E89A3C);
        }

        @Override
        protected void renderListSeparators(GuiGraphics g) {
            // cerceve zaten ayiriyor
        }
    }

    /** Calan parcanin yaninda ziplayan uc kucuk cubuk. */
    private static void drawEqualizer(GuiGraphics g, int x, int baseY, int color) {
        double t = System.currentTimeMillis() / 140.0;
        for (int i = 0; i < 3; i++) {
            int h = 2 + (int) Math.round((Math.sin(t + i * 1.9) * 0.5 + 0.5) * 7);
            g.fill(x + i * 3, baseY - h, x + i * 3 + 2, baseY, color);
        }
    }

    /** Yazisi kadar genis, kucuk bir dugme (cevirilerde metin uzunlugu degisebilir). */
    private Button smallButton(Component message, Runnable action) {
        return Button.builder(message, b -> action.run()).size(Math.max(60, font.width(message) + 12), 18).build();
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {}

    /** Mod basligi: jukebox ikonu, ad, parca sayisi, hepsini ac/kapat. */
    private class HeaderEntry extends Row {
        private static final ItemStack ICON = new ItemStack(Items.JUKEBOX);

        private final String modName;
        private final List<MusicTrack> tracks;
        private final Button allOn;
        private final Button allOff;

        HeaderEntry(String modName, List<MusicTrack> tracks) {
            this.modName = modName;
            this.tracks = tracks;
            this.allOn = smallButton(Lang.tr("screen.enable_all"), () -> setAll(true));
            this.allOff = smallButton(Lang.tr("screen.disable_all"), () -> setAll(false));
        }

        private void setAll(boolean enabled) {
            tracks.forEach(t -> MusicSettings.setEnabled(t.id(), enabled));
            MusicSettings.applyToPlayingSounds();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            // zemin + soldaki vurgu cizgisi
            g.fill(left - 3, top - 1, left + width + 3, top + height + 1, HEADER_BG);
            g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            g.fillGradient(left - 1, top + height, left + width + 3, top + height + 1, ACCENT_SOFT, ACCENT_SOFT);

            int midY = top + (height - 8) / 2;
            g.renderItem(ICON, left + 2, top + (height - 16) / 2);

            long off = tracks.stream().filter(t -> MusicSettings.isDisabled(t.id())).count();
            Component name = Component.literal(modName).withStyle(ChatFormatting.BOLD);
            int nameX = left + 22;
            g.drawString(font, name, nameX, midY, GOLD);
            MutableComponent info = Lang.count("tracks", tracks.size());
            if (off > 0) info.append(" · ").append(Lang.tr("count.off", off));
            g.drawString(font, info, nameX + font.width(name) + 6, midY, off > 0 ? 0xFFE07060 : MUTED);

            allOff.setPosition(left + width - allOff.getWidth(), top + (height - 18) / 2);
            allOn.setPosition(allOff.getX() - 4 - allOn.getWidth(), allOff.getY());
            allOn.active = off > 0;
            allOff.active = off < tracks.size();
            allOn.render(g, mouseX, mouseY, partialTick);
            allOff.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(allOn, allOff);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(allOn, allOff);
        }
    }

    /** Tek parca: [dinle] [ikon] ad ~~~ .......... [ses kaydiricisi] [Acik/Kapali] */
    private class TrackEntry extends Row {
        private static final int TOGGLE_W = 50;
        private static final int SLIDER_W = 96;

        private final MusicTrack track;
        private final boolean alternate;
        private final ItemStack icon;
        private final Button play;
        private final VolumeSlider slider;
        private final Button toggle;

        TrackEntry(MusicTrack track, boolean alternate) {
            this.track = track;
            this.alternate = alternate;
            this.icon = iconFor(track);
            this.play = Button.builder(Component.literal("▶"), b -> {
                if (MusicPreview.isPlaying(track.id())) MusicPreview.stop();
                else MusicPreview.play(track);
            }).size(20, 20).tooltip(Tooltip.create(Lang.tr("screen.play.tooltip"))).build();
            this.slider = new VolumeSlider(track);
            this.toggle = Button.builder(Component.empty(), b -> {
                MusicSettings.setEnabled(track.id(), MusicSettings.isDisabled(track.id()));
                MusicSettings.applyToPlayingSounds();
            }).size(TOGGLE_W, 20).build();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean enabled = !MusicSettings.isDisabled(track.id());
            boolean playing = MusicPreview.isPlaying(track.id());       // panelde onizleniyor
            boolean inGame = !playing && nowPlaying.contains(track);    // oyunda su an caliyor

            // satir zemini
            if (playing) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, 0x30E89A3C);
                g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            } else if (inGame) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, PLAYING_BG);
                g.fill(left - 3, top - 1, left - 1, top + height + 1, PLAYING);
            } else if (hovering) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_HOVER);
            } else if (alternate) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_ALT);
            }

            // dugmeler
            int by = top + (height - 20) / 2;
            play.setPosition(left, by);
            play.setMessage(playing
                    ? Component.literal("■").withStyle(s -> s.withColor(ACCENT & 0xFFFFFF))
                    : Component.literal("▶"));
            toggle.setPosition(left + width - TOGGLE_W, by);
            toggle.setMessage(enabled
                    ? Lang.tr("screen.on").withStyle(ChatFormatting.GREEN)
                    : Lang.tr("screen.off").withStyle(ChatFormatting.RED));
            slider.setPosition(toggle.getX() - 4 - SLIDER_W, by);
            slider.active = enabled;

            // ikon (kapaliysa karartilir)
            int iconX = left + 24;
            int iconY = top + (height - 16) / 2;
            g.renderItem(icon, iconX, iconY);
            if (!enabled) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(iconX, iconY, iconX + 16, iconY + 16, 0xA0100C08);
                g.pose().popPose();
            }

            // ad
            int nameX = iconX + 20;
            int textY = top + (height - 8) / 2;
            Component playingTag = Lang.tr("screen.playing_tag");
            int extraW = playing ? 12 : inGame ? 14 + font.width(playingTag) : 0;
            int nameMaxW = slider.getX() - 8 - nameX - extraW;
            String name = track.shortName();
            if (font.width(name) > nameMaxW) name = font.plainSubstrByWidth(name, nameMaxW - font.width("…")) + "…";
            if (enabled) {
                g.drawString(font, name, nameX, textY, playing ? GOLD : inGame ? PLAYING : TEXT);
            } else {
                g.drawString(font, Component.literal(name).withStyle(ChatFormatting.STRIKETHROUGH), nameX, textY, 0xFF6A6058);
            }
            int afterName = nameX + font.width(name) + 5;
            if (playing) {
                drawEqualizer(g, afterName, top + height - 5, ACCENT);
            } else if (inGame) {
                drawEqualizer(g, afterName, top + height - 5, PLAYING);
                g.drawString(font, playingTag, afterName + 12, textY, PLAYING);
            }

            play.render(g, mouseX, mouseY, partialTick);
            slider.render(g, mouseX, mouseY, partialTick);
            toggle.render(g, mouseX, mouseY, partialTick);

            // ad/ikon uzerine gelince ayrintilar
            if (hovering && mouseX >= iconX && mouseX < slider.getX() - 4) {
                setTooltipForNextRenderPass(details());
            }
        }

        private List<FormattedCharSequence> details() {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(track.shortName()).withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                    .append(Component.literal("  ").append(track.kind().displayName()).withStyle(ChatFormatting.GRAY)).getVisualOrderText());
            lines.add(Component.literal(track.id().toString()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            lines.add(Lang.tr("screen.events").withStyle(ChatFormatting.GRAY).getVisualOrderText());
            track.events().stream().map(Object::toString).sorted().limit(12)
                    .forEach(e -> lines.add(Component.literal("  " + e).getVisualOrderText()));
            if (track.events().size() > 12) {
                lines.add(Component.literal("  ... +" + (track.events().size() - 12)).withStyle(ChatFormatting.GRAY).getVisualOrderText());
            }
            return lines;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(play, slider, toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(play, slider, toggle);
        }
    }

    /** Satir ikonu: plaklar icin oyundaki plak esyasi, muzikler icin nota blogu, ortam sesleri icin yanki parcasi. */
    private static ItemStack iconFor(MusicTrack track) {
        return switch (track.kind()) {
            case DISC -> {
                // Plak esyasini bulmaya calis: "minecraft:records/cat" -> "minecraft:music_disc_cat";
                // bircok mod da esyaya ses olayiyla ayni adi verir ("cataclysm:ignis_music_disc").
                List<String> candidates = new ArrayList<>();
                candidates.add("music_disc_" + track.shortName());
                track.events().forEach(e -> candidates.add(e.getPath().replace('.', '_')));
                for (String path : candidates) {
                    ResourceLocation item = ResourceLocation.tryBuild(track.namespace(), path);
                    if (item != null && BuiltInRegistries.ITEM.containsKey(item)) yield new ItemStack(BuiltInRegistries.ITEM.get(item));
                }
                yield new ItemStack(Items.MUSIC_DISC_13);
            }
            case MUSIC -> new ItemStack(Items.NOTE_BLOCK);
            case OTHER -> new ItemStack(Items.ECHO_SHARD);
        };
    }

    /** %0-100 ses kaydiricisi. Degisiklik calan seslere aninda uygulanir. */
    private static class VolumeSlider extends AbstractSliderButton {
        private final MusicTrack track;

        VolumeSlider(MusicTrack track) {
            super(0, 0, TrackEntry.SLIDER_W, 20, Component.empty(), MusicSettings.get(track.id()).volume() / 100.0);
            this.track = track;
            updateMessage();
        }

        private int percent() {
            return Mth.clamp((int) Math.round(value * 100), 0, 100);
        }

        @Override
        protected void updateMessage() {
            int p = percent();
            setMessage(Lang.tr("screen.volume", p).withStyle(p == 100 ? ChatFormatting.WHITE : ChatFormatting.YELLOW));
        }

        @Override
        protected void applyValue() {
            MusicSettings.setVolume(track.id(), percent());
            MusicSettings.applyToPlayingSounds();
        }
    }
}
