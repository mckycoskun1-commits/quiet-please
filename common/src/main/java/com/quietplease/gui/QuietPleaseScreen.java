package com.quietplease.gui;

import com.quietplease.Lang;
import com.quietplease.mixin.MusicManagerAccessor;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicControl;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicPreview;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.NowPlaying;
import com.quietplease.music.Playlist;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.resources.Identifier;
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

    /** Ikondaki sicak turuncu isiltiyla uyumlu vurgu rengi. */
    static final int ACCENT = 0xFFE89A3C;
    static final int ACCENT_SOFT = 0x80E89A3C;
    static final int PANEL_BG = 0xB0100C08;
    static final int HEADER_BG = 0x803A2210;
    static final int ROW_ALT = 0x10FFFFFF;
    static final int ROW_HOVER = 0x24FFD9A0;
    static final int GOLD = 0xFFFFC966;
    static final int TEXT = 0xFFF2E6C9;
    static final int MUTED = 0xFF9A8C78;

    static final int PLAYING = 0xFF7EE08A;      // oyunda su an calan parca (yesil)
    static final int PLAYING_BG = 0x2470D080;

    private static final int ROW_HEIGHT = 24;
    private static final int NOW_PLAYING_Y = 56;
    private static final int NOW_PLAYING_H = 16;
    private static final int TOP = 78;
    private static final int BOTTOM_MARGIN = 36;
    private static final int MAX_ROW_WIDTH = 420;
    /** Mod filtresindeki "★ Calma listesi" secenegi (bir mod kimligi olamaz). */
    private static final String PLAYLIST_FILTER = "#playlist";

    @Nullable
    private final Screen parent;

    private Map<String, List<MusicTrack>> library = Map.of();
    private List<MusicTrack> allTracks = List.of();
    private Map<Identifier, MusicTrack> tracksById = Map.of();
    /** Oyunda su an calan parcalar (her tick'te guncellenir). */
    private List<MusicTrack> nowPlaying = List.of();
    private int nowPlayingIndex = 0;
    private TrackList list;
    @Nullable
    private CycleButton<String> modButton;
    private List<String> modValues = List.of();
    @Nullable
    private Button skip;

    // Filtreler (ekran yeniden boyutlaninca korunsun diye alanlarda tutuluyor)
    private String query = "";
    private String modFilter = ""; // "" = tum modlar
    private boolean showOther = ModOptions.showAmbient;

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

        // Baslikta: solda Ayarlar (ortadaki logoya carpmasin diye satirin kenarlarinda)
        Component settingsLabel = Lang.tr("settings.button");
        addRenderableWidget(Button.builder(settingsLabel, b -> minecraft.gui.setScreen(new QuietPleaseSettingsScreen(this)))
                .bounds(x, 6, Math.max(60, font.width(settingsLabel) + 12), 20)
                .tooltip(Tooltip.create(Lang.tr("settings.button.tooltip")))
                .build());
        // ... sagda Calma listesi
        Component playlistLabel = Lang.tr("playlist.button");
        int playlistW = Math.max(60, font.width(playlistLabel) + 12);
        addRenderableWidget(Button.builder(playlistLabel, b -> minecraft.gui.setScreen(new QuietPlaylistScreen(this)))
                .bounds(x + rowW - playlistW, 6, playlistW, 20)
                .tooltip(Tooltip.create(Lang.tr("playlist.open.tooltip")))
                .build());

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
        mods.add(PLAYLIST_FILTER);
        mods.addAll(library.keySet());
        if (!mods.contains(modFilter)) modFilter = "";
        modValues = mods;
        modButton = addRenderableWidget(CycleButton.<String>builder(m -> m.isEmpty() ? Lang.tr("screen.mod.all")
                        : m.equals(PLAYLIST_FILTER) ? Lang.tr("screen.mod.playlist").withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                        : Component.literal(MusicLibrary.modName(m)), modFilter)
                .withValues(mods)
                .withTooltip(v -> Tooltip.create(Lang.tr("screen.mod.tooltip")))
                .create(x + searchW + 4, y, modW, 20, Lang.tr("screen.mod"), (b, value) -> {
                    modFilter = value;
                    rebuildList();
                }));

        addRenderableWidget(CycleButton.booleanBuilder(
                        Lang.tr("screen.ambient.show").withStyle(ChatFormatting.GREEN),
                        Lang.tr("screen.ambient.hide").withStyle(ChatFormatting.GRAY), showOther)
                .withTooltip(v -> Tooltip.create(Lang.tr("screen.ambient.tooltip")))
                .create(x + searchW + 4 + modW + 4, y, otherW, 20, Lang.tr("screen.ambient"), (b, value) -> {
                    showOther = value;
                    rebuildList();
                }));

        // Liste
        list = new TrackList(minecraft, width, height - TOP - BOTTOM_MARGIN, TOP);
        addRenderableWidget(list);
        rebuildList();

        // "Su an calan" cubugunun sagi: Atla
        Component skipLabel = Lang.tr("screen.skip");
        int skipW = Math.max(46, font.width(skipLabel) + 10);
        skip = addRenderableWidget(Button.builder(skipLabel, b -> MusicControl.skip())
                .bounds(barRight() - skipW, NOW_PLAYING_Y, skipW, NOW_PLAYING_H)
                .tooltip(Tooltip.create(Lang.tr("screen.skip.tooltip")))
                .build());

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
        boolean favoritesOnly = modFilter.equals(PLAYLIST_FILTER);
        // En ustte Favoriler: yalnizca en az bir favori varsa
        if (modFilter.isEmpty() || favoritesOnly) {
            List<MusicTrack> favorites = Playlist.favorites().stream()
                    .map(tracksById::get)
                    .filter(t -> t != null && matches(t, q))
                    .filter(t -> showOther || favoritesOnly || t.kind() != MusicTrack.Kind.OTHER)
                    .collect(Collectors.toList());
            if (!favorites.isEmpty()) {
                list.add(new HeaderEntry(Lang.tr("screen.favorites").getString(), favorites, true));
                for (int i = 0; i < favorites.size(); i++) list.add(new TrackEntry(favorites.get(i), i % 2 == 1));
            }
        }
        if (favoritesOnly) {
            list.setScrollAmount(0);
            return;
        }
        library.forEach((mod, tracks) -> {
            if (!modFilter.isEmpty() && !modFilter.equals(mod)) return;
            List<MusicTrack> visible = tracks.stream()
                    .filter(t -> showOther || t.kind() != MusicTrack.Kind.OTHER)
                    .filter(t -> matches(t, q))
                    .collect(Collectors.toList());
            if (visible.isEmpty()) return;
            list.add(new HeaderEntry(MusicLibrary.modName(mod), visible, false));
            for (int i = 0; i < visible.size(); i++) list.add(new TrackEntry(visible.get(i), i % 2 == 1));
        });
        list.setScrollAmount(0);
    }

    private static boolean matches(MusicTrack t, String q) {
        return q.isEmpty()
                || t.id().toString().toLowerCase(Locale.ROOT).contains(q)
                || MusicLibrary.modName(t.namespace()).toLowerCase(Locale.ROOT).contains(q);
    }

    // ------------------------------------------------------------------ cizim ve yasam dongusu

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        renderHeader(g);
        renderNowPlaying(g, mouseX, mouseY);
        if (list.children().isEmpty()) {
            g.centeredText(font, Lang.tr("screen.no_results"), width / 2, TOP + 24, MUTED);
        }
    }

    /** Logo + baslik + ozet satiri. */
    private void renderHeader(GuiGraphicsExtractor g) {
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

        drawLogo(g, x, y, logo);
        g.text(font, title, x + logo + 6, y + 2, GOLD);
        g.text(font, summary, x + logo + 6, y + 13, MUTED);
    }

    // ------------------------------------------------------------------ "su an calan" cubugu

    private int barLeft() {
        return list.getRowLeft() - 6;
    }

    private int barRight() {
        return list.scrollBarX() + 10;
    }

    /** Cubugun sag kenari: Atla dugmesinin hemen solu. */
    private int barTextRight() {
        return skip != null ? skip.getX() - 2 : barRight();
    }

    private boolean overNowPlaying(double mouseX, double mouseY) {
        return mouseX >= barLeft() && mouseX < barTextRight()
                && mouseY >= NOW_PLAYING_Y && mouseY < NOW_PLAYING_Y + NOW_PLAYING_H;
    }

    /** Listenin ustunde: oyunda su an hangi parca caliyor (oyuncu muzigin adini bilmese de bulabilsin diye). */
    private void renderNowPlaying(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int left = barLeft();
        int right = barTextRight();
        // Atla sadece oyunun arka plan muzigi calarken (ya da sirada beklerken) ise yarar
        skip.active = ((MusicManagerAccessor) minecraft.getMusicManager()).quietplease$getCurrentMusic() != null;
        int top = NOW_PLAYING_Y;
        boolean hover = !nowPlaying.isEmpty() && overNowPlaying(mouseX, mouseY);
        g.fill(left, top, right, top + NOW_PLAYING_H, hover ? 0x5070D080 : PLAYING_BG);
        g.fill(left, top, left + 2, top + NOW_PLAYING_H, nowPlaying.isEmpty() ? MUTED : PLAYING);
        int textY = top + (NOW_PLAYING_H - 8) / 2;

        if (nowPlaying.isEmpty()) {
            g.text(font, Lang.tr("screen.now_playing.none"), left + 8, textY, MUTED);
            return;
        }
        MusicTrack shown = nowPlaying.get(Math.floorMod(nowPlayingIndex, nowPlaying.size()));
        drawEqualizer(g, left + 7, top + NOW_PLAYING_H - 4, PLAYING);
        MutableComponent line = Lang.tr("screen.now_playing").append(" ")
                .append(Component.literal(shown.shortName()).withStyle(s -> s.withColor(PLAYING & 0xFFFFFF)))
                .append(Component.literal("  " + MusicLibrary.modName(shown.namespace())).withStyle(ChatFormatting.GRAY));
        if (nowPlaying.size() > 1) line.append(Component.literal("  ")).append(Lang.tr("screen.now_playing.more", nowPlaying.size() - 1).withStyle(ChatFormatting.DARK_GRAY));
        // Uzun adlar cubugun (ve Atla dugmesinin) ustune tasmasin
        int maxW = right - 4 - (left + 20);
        if (font.width(line) > maxW) {
            g.text(font, font.plainSubstrByWidth(line.getString(), maxW - font.width("…")) + "…", left + 20, textY, PLAYING);
        } else {
            g.text(font, line, left + 20, textY, TEXT);
        }
        if (hover) g.setTooltipForNextFrame(font, Lang.tr("screen.now_playing.tooltip"), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        // Mod filtresi: sol tik bir sonraki moda gecer (Minecraft'in kendi davranisi), sag tik bir oncekine doner.
        if (button == 1 && modButton != null && modButton.isMouseOver(mouseX, mouseY) && !modValues.isEmpty()) {
            int i = modValues.indexOf(modButton.getValue());
            String previous = modValues.get(Math.floorMod(i - 1, modValues.size()));
            modButton.setValue(previous); // setValue geri cagrimi tetiklemez, filtreyi kendimiz uyguluyoruz
            modFilter = previous;
            rebuildList();
            modButton.playDownSound(minecraft.getSoundManager());
            return true;
        }
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
        return super.mouseClicked(event, doubleClick);
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
        minecraft.gui.setScreen(parent);
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
        protected int scrollBarX() {
            return getRowLeft() + getRowWidth() + 8;
        }

        /** Oyunun dokulu liste zemini yerine koyu, turuncu cerceveli bir panel. */
        @Override
        protected void extractListBackground(GuiGraphicsExtractor g) {
            int left = getRowLeft() - 6;
            int right = scrollBarX() + 6 + 4;
            int top = getY() - 2;
            int bottom = getBottom() + 2;
            g.fill(left, top, right, bottom, PANEL_BG);
            g.outline(left - 1, top - 1, right - left + 2, bottom - top + 2, ACCENT_SOFT);
            // ust kenarda hafif bir isilti
            g.fillGradient(left, top, right, top + 6, 0x30E89A3C, 0x00E89A3C);
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor g) {
            // cerceve zaten ayiriyor
        }
    }

    static ItemStack stack(net.minecraft.world.item.Item item) {
        return item.builtInRegistryHolder().areComponentsBound() ? new ItemStack(item) : ItemStack.EMPTY;
    }

    /**
     * Baslik logosu: ince turuncu cerceveli koyu kare icinde oyunun kendi Jukebox esyasi.
     * (Ayri bir resim dosyasi yok; boylece panelde sadece Minecraft'in kendi cizimleri gorunur.)
     */
    static void drawLogo(GuiGraphicsExtractor g, int x, int y, int size) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, ACCENT_SOFT);
        g.fill(x, y, x + size, y + size, HEADER_BG);
        int inner = size - 4;
        g.pose().pushMatrix();
        g.pose().translate(x + 2, y + 2);
        g.pose().scale(inner / 16.0F, inner / 16.0F);
        g.item(stack(Items.JUKEBOX), 0, 0);
        g.pose().popMatrix();
    }

    /** Calan parcanin yaninda ziplayan uc kucuk cubuk. */
    static void drawEqualizer(GuiGraphicsExtractor g, int x, int baseY, int color) {
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

        private final String modName;
        private final List<MusicTrack> tracks;
        private final boolean favorites;
        private final Button allOn;
        private final Button allOff;

        HeaderEntry(String modName, List<MusicTrack> tracks, boolean favorites) {
            this.modName = modName;
            this.tracks = tracks;
            this.favorites = favorites;
            this.allOn = smallButton(Lang.tr("screen.enable_all"), () -> setAll(true));
            this.allOff = smallButton(Lang.tr("screen.disable_all"), () -> setAll(false));
        }

        private void setAll(boolean enabled) {
            tracks.forEach(t -> MusicSettings.setEnabled(t.id(), enabled));
            MusicSettings.applyToPlayingSounds();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int top = getContentY();
            int left = getContentX();
            int width = getContentWidth();
            int height = getContentHeight();
            // zemin + soldaki vurgu cizgisi
            g.fill(left - 3, top - 1, left + width + 3, top + height + 1, HEADER_BG);
            g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            g.fillGradient(left - 1, top + height, left + width + 3, top + height + 1, ACCENT_SOFT, ACCENT_SOFT);

            int midY = top + (height - 8) / 2;
            g.item(stack(favorites ? Items.NETHER_STAR : Items.JUKEBOX), left + 2, top + (height - 16) / 2);

            long off = tracks.stream().filter(t -> MusicSettings.isDisabled(t.id())).count();
            Component name = Component.literal(favorites ? "★ " + modName : modName).withStyle(ChatFormatting.BOLD);
            int nameX = left + 22;
            g.text(font, name, nameX, midY, GOLD);
            MutableComponent info = Lang.count("tracks", tracks.size());
            if (off > 0) info.append(" · ").append(Lang.tr("count.off", off));
            g.text(font, info, nameX + font.width(name) + 6, midY, off > 0 ? 0xFFE07060 : MUTED);

            allOff.setPosition(left + width - allOff.getWidth(), top + (height - 18) / 2);
            allOn.setPosition(allOff.getX() - 4 - allOn.getWidth(), allOff.getY());
            allOn.active = off > 0;
            allOff.active = off < tracks.size();
            allOn.extractRenderState(g, mouseX, mouseY, partialTick);
            allOff.extractRenderState(g, mouseX, mouseY, partialTick);
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
        private final Button star;
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
            this.star = Button.builder(Component.empty(), b -> {
                Playlist.toggleFavorite(track.id());
                MusicSettings.save();
                // Favoriler grubu en ustte: yildizlanan parca oraya gelsin, yildizi kaldirilan oradan kalksin
                double scroll = list.scrollAmount();
                rebuildList();
                list.setScrollAmount(scroll);
            }).size(20, 20).build();
            this.slider = new VolumeSlider(track);
            this.toggle = Button.builder(Component.empty(), b -> {
                MusicSettings.setEnabled(track.id(), MusicSettings.isDisabled(track.id()));
                MusicSettings.applyToPlayingSounds();
            }).size(TOGGLE_W, 20).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int top = getContentY();
            int left = getContentX();
            int width = getContentWidth();
            int height = getContentHeight();
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
            boolean starred = Playlist.isFavorite(track.id());
            star.setPosition(left + 22, by);
            star.setMessage(starred
                    ? Component.literal("★").withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                    : Component.literal("☆").withStyle(ChatFormatting.GRAY));
            star.setTooltip(Tooltip.create(Lang.tr(starred ? "screen.star.remove" : "screen.star.add")));
            toggle.setPosition(left + width - TOGGLE_W, by);
            toggle.setMessage(enabled
                    ? Lang.tr("screen.on").withStyle(ChatFormatting.GREEN)
                    : Lang.tr("screen.off").withStyle(ChatFormatting.RED));
            slider.setPosition(toggle.getX() - 4 - SLIDER_W, by);
            slider.active = enabled;

            // ikon (kapaliysa karartilir)
            int iconX = left + 46;
            int iconY = top + (height - 16) / 2;
            g.item(icon, iconX, iconY);
            if (!enabled) {
                g.pose().pushMatrix();
                g.pose().translate(0, 0);
                g.fill(iconX, iconY, iconX + 16, iconY + 16, 0xA0100C08);
                g.pose().popMatrix();
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
                g.text(font, name, nameX, textY, playing ? GOLD : inGame ? PLAYING : TEXT);
            } else {
                g.text(font, Component.literal(name).withStyle(ChatFormatting.STRIKETHROUGH), nameX, textY, 0xFF6A6058);
            }
            int afterName = nameX + font.width(name) + 5;
            if (playing) {
                drawEqualizer(g, afterName, top + height - 5, ACCENT);
            } else if (inGame) {
                drawEqualizer(g, afterName, top + height - 5, PLAYING);
                g.text(font, playingTag, afterName + 12, textY, PLAYING);
            }

            play.extractRenderState(g, mouseX, mouseY, partialTick);
            star.extractRenderState(g, mouseX, mouseY, partialTick);
            slider.extractRenderState(g, mouseX, mouseY, partialTick);
            toggle.extractRenderState(g, mouseX, mouseY, partialTick);

            // ad/ikon uzerine gelince ayrintilar
            if (hovering && mouseX >= iconX && mouseX < slider.getX() - 4) {
                g.setTooltipForNextFrame(font, details(), mouseX, mouseY);
            }
        }

        private List<FormattedCharSequence> details() {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(track.shortName()).withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                    .append(Component.literal("  ").append(track.kind().displayName()).withStyle(ChatFormatting.GRAY)).getVisualOrderText());
            if (!Minecraft.getInstance().hasControlDown()) {
                lines.add(Lang.tr("screen.details_hint").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
                return lines;
            }
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
            return List.of(play, star, slider, toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(play, star, slider, toggle);
        }
    }

    /** Satir ikonu: plaklar icin oyundaki plak esyasi, muzikler icin nota blogu, ortam sesleri icin yanki parcasi. */
    static ItemStack iconFor(MusicTrack track) {
        return switch (track.kind()) {
            case DISC -> {
                // Plak esyasini bulmaya calis: "minecraft:records/cat" -> "minecraft:music_disc_cat";
                // bircok mod da esyaya ses olayiyla ayni adi verir ("cataclysm:ignis_music_disc").
                List<String> candidates = new ArrayList<>();
                candidates.add("music_disc_" + track.shortName());
                track.events().forEach(e -> candidates.add(e.getPath().replace('.', '_')));
                for (String path : candidates) {
                    Identifier item = Identifier.tryBuild(track.namespace(), path);
                    if (item != null && BuiltInRegistries.ITEM.containsKey(item)) yield stack(BuiltInRegistries.ITEM.getValue(item));
                }
                yield stack(Items.MUSIC_DISC_13);
            }
            case MUSIC -> stack(Items.NOTE_BLOCK);
            case OTHER -> stack(Items.ECHO_SHARD);
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
