package com.quietplease.gui;

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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Calma listesi paneli: bir muzik calar gibi, en ustte calan sarki (ilerleme cubuguyla), altinda listenin tamami
 * numarali; calan sarki yerinde durur ve basinda yesil cizgi vardir. Sira degistirme (▲/▼), cikarma (✕), hemen
 * calma (▶), dongu (∞), atlama, karistirma ve "Sarki ekle" sekmesinde tum muziklerden ekleme.
 *
 * Ustteki ◀ ▶ dugmeleri sira ile kayitli listeler ("Sakin", "Insaat" gibi) arasinda gecer; + yeni liste acar.
 * Kayitli listede dongu dugmesi yoktur (parcalar zaten listede kalir); alt satirda Cal/Durdur, Karistir,
 * Yeniden adlandir ve Sil vardir.
 */
public class QuietPlaylistScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int BAR_Y = 54;
    private static final int BAR_H = 20;
    private static final int TOP = 80;
    private static final int BOTTOM_MARGIN = 46;
    private static final int MAX_ROW_WIDTH = 420;
    /** Sagdaki "Onerilenler" paneli ve yanindaki siranin en dar hali. */
    private static final int SIDE_GAP = 22;
    private static final int MIN_QUEUE_W = 240;
    /** Onerilerde kullanilan bilinen vanilla parcalar (dosya adlari). */
    private static final Set<String> POPULAR = Set.of(
            "sweden", "wet_hands", "subwoofer_lullaby", "haggstrom", "mice_on_venus", "danny", "clark", "dry_hands",
            "minecraft", "living_mice", "key", "oxygene", "equinoxe", "aria_math", "biome_fest", "moog_city_2",
            "beginning_2", "floating_trees", "mutation", "comforting_memories", "stand_tall", "wending",
            "left_to_bloom", "infinite_amethyst", "one_more_day", "floating_dream", "an_ordinary_day", "featherfall",
            "taswell", "dreiton", "haunt_muskie", "blind_spots", "pigstep", "otherside", "rubedo", "chrysopoeia");

    @Nullable
    private final Screen parent;
    /** Oneri adaylari: favoriler, daha once eklenenler ve bilinen vanilla parcalar, karisik (panel acilinca bir kez). */
    @Nullable
    private List<Identifier> suggestionPool;
    private final List<Button> suggestionButtons = new ArrayList<>();
    @Nullable
    private Button viewTab;
    /** Gosterilen liste: null = sira. */
    @Nullable
    private Playlist.SavedList view;

    private Map<String, List<MusicTrack>> library = Map.of();
    private Map<Identifier, MusicTrack> tracksById = Map.of();
    private RowList list;
    @Nullable
    private CycleButton<String> modButton;
    private List<String> modValues = List.of();
    private Button skip;
    private int shownVersion = -1;

    // Ekran yeniden boyutlaninca korunsun diye alanlarda
    private boolean addMode = false;
    private String query = "";
    private String modFilter = "";

    public QuietPlaylistScreen(@Nullable Screen parent) {
        super(Lang.tr("playlist.title"));
        this.parent = parent;
        // Sira bossa ve bir liste caliyorsa o listeyle acilsin
        if (Playlist.isEmpty()) this.view = Playlist.activeList();
    }

    /** Belirli bir kayitli listeyle acar. */
    public QuietPlaylistScreen(@Nullable Screen parent, @Nullable Playlist.SavedList view) {
        this(parent);
        this.view = view;
    }

    // ------------------------------------------------------------------ gosterilen liste (sira ya da kayitli liste)

    private List<Identifier> viewTracks() {
        return view == null ? Playlist.tracks() : view.tracks();
    }

    private int viewSize() {
        return view == null ? Playlist.size() : view.size();
    }

    private boolean inView(Identifier id) {
        return view == null ? Playlist.contains(id) : view.contains(id);
    }

    private void addToView(Identifier id) {
        if (view == null) Playlist.add(id);
        else Playlist.listAdd(view, id);
    }

    private void toggleInView(Identifier id) {
        if (view == null) Playlist.toggle(id);
        else Playlist.listToggle(view, id);
    }

    /** Gosterilen listeden su an calan parca. */
    @Nullable
    private Identifier viewCurrent() {
        return view == null ? Playlist.currentTrack() : Playlist.listCurrentTrack(view);
    }

    /** ◀ / ▶: sira, sonra kayitli listeler sirasiyla. */
    private void cycleView(int step) {
        List<Playlist.SavedList> lists = Playlist.lists();
        int i = view == null ? -1 : lists.indexOf(view);
        int n = lists.size() + 1;
        int next = Math.floorMod(i + 1 + step, n) - 1;
        view = next < 0 ? null : lists.get(next);
        shownVersion = -1;
        rebuildWidgets();
    }

    private Component viewLabel(int maxW) {
        if (view == null) return fit(Lang.tr("playlist.tab.queue", Playlist.size()), maxW);
        boolean playing = view == Playlist.activeList();
        String count = " (" + view.size() + ")";
        String name = view.name();
        String prefix = playing ? "♪ " : "";
        int room = maxW - font.width(prefix + count + "…");
        if (font.width(prefix + name + count) > maxW) name = font.plainSubstrByWidth(name, Math.max(0, room)) + "…";
        return Component.literal(prefix + name + count).withStyle(s -> s.withColor((playing ? PLAYING : TEXT) & 0xFFFFFF));
    }

    // ------------------------------------------------------------------ kurulum

    /** Oneriler paneli sigiyor mu (yalnizca sira sekmesinde)? */
    private boolean sideShown() {
        return !addMode && width - 20 - sideW() - SIDE_GAP >= MIN_QUEUE_W;
    }

    private int sideW() {
        return Math.max(100, Math.min(140, (width - 20) / 3));
    }

    /** Siranin (listenin) genisligi. */
    private int rowW() {
        int available = width - 20;
        return sideShown() ? Math.min(MAX_ROW_WIDTH, available - SIDE_GAP - sideW()) : Math.min(available, MAX_ROW_WIDTH);
    }

    /** Sira + oneriler paneli: ust ve alt satirdaki dugmeler bu genislige yayilir. */
    private int blockW() {
        return rowW() + (sideShown() ? SIDE_GAP + sideW() : 0);
    }

    private int rowX() {
        return (width - blockW()) / 2;
    }

    private int sideX() {
        return rowX() + rowW() + SIDE_GAP;
    }

    @Override
    protected void init() {
        library = MusicLibrary.scan();
        tracksById = library.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(MusicTrack::id, t -> t, (a, b) -> a));
        int x = rowX();
        int rowW = blockW();

        if (view != null && !Playlist.lists().contains(view)) view = null;

        // Ust satir: [◀] sira / liste [▶] [+] [Sarki ekle] .......... [Calma listesi modu]
        int y = 30;
        int labelW = 120;
        Button prev = addRenderableWidget(iconButton("◀", Lang.tr("playlist.view.prev.tooltip"), () -> cycleView(-1)));
        prev.setRectangle(20, 20, x, y);
        viewTab = addRenderableWidget(Button.builder(viewLabel(labelW - 8), b -> {
            addMode = false;
            rebuildWidgets();
        }).bounds(x + 22, y, labelW, 20).tooltip(Tooltip.create(Lang.tr("playlist.view.tooltip"))).build());
        Button next = addRenderableWidget(iconButton("▶", Lang.tr("playlist.view.next.tooltip"), () -> cycleView(1)));
        next.setRectangle(20, 20, x + 24 + labelW, y);
        prev.active = next.active = !Playlist.lists().isEmpty();
        Button create = addRenderableWidget(iconButton("+", Lang.tr("playlist.new.tooltip"), () ->
                minecraft.gui.setScreen(new PlaylistNameScreen(this, null, name -> {
                    Playlist.SavedList made = Playlist.createList(name);
                    if (made != null) {
                        view = made;
                        addMode = true; // yeni listeye hemen sarki eklensin
                        MusicSettings.save();
                    }
                }))));
        create.setRectangle(20, 20, x + 48 + labelW, y);
        create.active = Playlist.lists().size() < Playlist.LISTS_MAX;
        int addX = x + 72 + labelW;
        Button addTab = addRenderableWidget(Button.builder(Lang.tr("playlist.tab.add"), b -> {
            addMode = true;
            rebuildWidgets();
        }).bounds(addX, y, 90, 20).build());
        viewTab.active = addMode;
        addTab.active = !addMode;
        int modeW = Math.max(80, Math.min(150, x + rowW - (addX + 94)));
        addRenderableWidget(CycleButton.onOffBuilder(ModOptions.playlistMode)
                .withTooltip(v -> Tooltip.create(Lang.tr("settings.playlist_mode.tooltip")))
                .create(x + rowW - modeW, y, modeW, 20, Lang.tr("settings.playlist_mode"), (b, v) -> {
                    ModOptions.playlistMode = v;
                    MusicSettings.save();
                }));

        // Ikinci satir: sira sekmesinde "su an calan" cubugu + Atla; ekleme sekmesinde arama + mod filtresi
        y = BAR_Y;
        if (addMode) {
            int modW = 150;
            EditBox search = new EditBox(font, x + 1, y + 1, rowW - modW - 6, 18, Lang.tr("screen.search"));
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
            modButton = addRenderableWidget(CycleButton.<String>builder(m -> m.isEmpty() ? Lang.tr("screen.mod.all") : Component.literal(MusicLibrary.modName(m)), modFilter)
                    .withValues(mods)
                    .withTooltip(v -> Tooltip.create(Lang.tr("screen.mod.tooltip")))
                    .create(x + rowW - modW, y, modW, 20, Lang.tr("screen.mod"), (b, value) -> {
                        modFilter = value;
                        rebuildList();
                    }));
        } else {
            modButton = null;
            Component skipLabel = Lang.tr("screen.skip");
            int skipW = Math.max(50, font.width(skipLabel) + 12);
            skip = addRenderableWidget(Button.builder(skipLabel, b -> {
                        com.quietplease.music.MusicControl.skip();
                        rebuildList();
                    }).bounds(x + rowW - skipW, y, skipW, BAR_H)
                    .tooltip(Tooltip.create(Lang.tr("screen.skip.tooltip")))
                    .build());
        }

        list = new RowList(minecraft, rowW() + 40, height - TOP - BOTTOM_MARGIN, TOP);
        list.setX(x - 20);
        addRenderableWidget(list);
        if (sideShown()) addRenderableOnly(this::renderSuggestionPanel);
        rebuildList();

        // Alt satir
        int by = height - 28;
        if (addMode) {
            addRenderableWidget(Button.builder(Lang.tr("playlist.back"), b -> {
                addMode = false;
                rebuildWidgets();
            }).bounds(width / 2 - 154, by, 150, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 + 4, by, 150, 20).build());
        } else if (view != null) {
            Playlist.SavedList list = view;
            int bw = (rowW - 16) / 5;
            boolean playingList = list == Playlist.activeList();
            Button play = addRenderableWidget(Button.builder(Lang.tr(playingList ? "playlist.list.stop" : "playlist.list.play"), b -> {
                        if (list == Playlist.activeList()) Playlist.stopList();
                        else Playlist.startList(list, -1);
                        MusicSettings.save();
                        rebuildWidgets();
                    }).bounds(x, by, bw, 20)
                    .tooltip(Tooltip.create(Lang.tr(playingList ? "playlist.list.stop.tooltip" : "playlist.list.play.tooltip")))
                    .build());
            play.active = playingList || !list.isEmpty();
            addRenderableWidget(Button.builder(Lang.tr("playlist.shuffle"), b -> {
                        Playlist.listShuffle(list);
                        rebuildList();
                    }).bounds(x + bw + 4, by, bw, 20)
                    .tooltip(Tooltip.create(Lang.tr("playlist.list.shuffle.tooltip")))
                    .build()).active = list.size() > 1;
            addRenderableWidget(Button.builder(Lang.tr("playlist.list.rename"), b -> minecraft.gui.setScreen(
                    new PlaylistNameScreen(this, list, name -> {
                        Playlist.renameList(list, name);
                        MusicSettings.save();
                    }))).bounds(x + 2 * (bw + 4), by, bw, 20).build());
            addRenderableWidget(Button.builder(Lang.tr("playlist.list.delete"), b -> minecraft.gui.setScreen(new ConfirmScreen(ok -> {
                        if (ok) {
                            Playlist.deleteList(list);
                            view = null;
                            MusicSettings.save();
                        }
                        minecraft.gui.setScreen(this);
                    }, Lang.tr("playlist.list.delete.confirm", list.name()),
                    Lang.tr("playlist.list.delete.confirm.message", list.size()))))
                    .bounds(x + 3 * (bw + 4), by, bw, 20)
                    .build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(x + 4 * (bw + 4), by, bw, 20).build());
        } else {
            int bw = (rowW - 8) / 3;
            addRenderableWidget(Button.builder(Lang.tr("playlist.shuffle"), b -> {
                        Playlist.shuffle();
                        rebuildList();
                    }).bounds(x, by, bw, 20)
                    .tooltip(Tooltip.create(Lang.tr("playlist.shuffle.tooltip")))
                    .build());
            addRenderableWidget(Button.builder(Lang.tr("playlist.clear"), b -> minecraft.gui.setScreen(new ConfirmScreen(ok -> {
                        if (ok) Playlist.clear();
                        minecraft.gui.setScreen(this);
                    }, Lang.tr("playlist.clear.confirm"), Lang.tr("playlist.clear.confirm.message", Playlist.size()))))
                    .bounds(x + bw + 4, by, bw, 20)
                    .build()).active = !Playlist.isEmpty();
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(x + 2 * (bw + 4), by, bw, 20).build());
        }
    }

    private void rebuildList() {
        if (list == null) return;
        double scroll = list.scrollAmount();
        boolean sameView = shownVersion != -1;
        list.clear();
        if (addMode) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            library.forEach((mod, tracks) -> {
                if (!modFilter.isEmpty() && !modFilter.equals(mod)) return;
                String modName = MusicLibrary.modName(mod);
                List<MusicTrack> visible = tracks.stream()
                        .filter(t -> t.kind() != MusicTrack.Kind.NOTE)
                        .filter(t -> ModOptions.showAmbient || t.kind() != MusicTrack.Kind.OTHER || inView(t.id()))
                        .filter(t -> q.isEmpty()
                                || t.id().toString().toLowerCase(Locale.ROOT).contains(q)
                                || modName.toLowerCase(Locale.ROOT).contains(q))
                        .toList();
                if (visible.isEmpty()) return;
                list.add(new ModHeader(modName, visible.size()));
                for (int i = 0; i < visible.size(); i++) list.add(new AddRow(visible.get(i), i % 2 == 1));
            });
        } else {
            for (int i = 0; i < viewSize(); i++) list.add(new QueueRow(i));
        }
        shownVersion = Playlist.version();
        list.setScrollAmount(sameView ? scroll : 0);
        if (viewTab != null) viewTab.setMessage(viewLabel(viewTab.getWidth() - 8));
        refreshSuggestions();
    }

    // ------------------------------------------------------------------ oneriler

    private List<Identifier> suggestionPool() {
        if (suggestionPool == null) {
            Set<Identifier> pool = new LinkedHashSet<>(Playlist.favorites());
            pool.addAll(Playlist.history());
            tracksById.values().stream()
                    .filter(t -> t.namespace().equals("minecraft") && POPULAR.contains(t.shortName()))
                    .map(MusicTrack::id)
                    .forEach(pool::add);
            List<Identifier> mixed = new ArrayList<>(pool);
            Collections.shuffle(mixed);
            suggestionPool = mixed;
        }
        return suggestionPool;
    }

    /** Sagdaki paneldeki dugmeler: siradakiler, kapalilar ve yuklu olmayanlar haric, sigdigi kadar. */
    private void refreshSuggestions() {
        suggestionButtons.forEach(this::removeWidget);
        suggestionButtons.clear();
        if (!sideShown() || list == null) return;
        int x = sideX();
        int y = TOP + 24;
        int bottom = list.getBottom() - 2;
        for (Identifier id : suggestionPool()) {
            if (y + 18 > bottom) break;
            MusicTrack track = tracksById.get(id);
            if (track == null || inView(id) || MusicSettings.isDisabled(id)) continue;
            boolean favorite = Playlist.isFavorite(id);
            String reason = favorite ? "playlist.suggest.favorite"
                    : Playlist.history().contains(id) ? "playlist.suggest.history" : "playlist.suggest.popular";
            Component label = fit(Component.literal("+ " + track.title())
                    .withStyle(s -> s.withColor((favorite ? GOLD : TEXT) & 0xFFFFFF)), sideW() - 10);
            Component tooltip = Component.literal(track.title()).withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                    .append(Component.literal("\n" + MusicLibrary.modName(track.namespace()) + " · ").withStyle(ChatFormatting.GRAY))
                    .append(Lang.tr(reason).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n").append(Lang.tr(view == null ? "playlist.add.tooltip" : "playlist.list.add.tooltip"))
                            .withStyle(ChatFormatting.DARK_GRAY));
            Button b = Button.builder(label, btn -> {
                addToView(id);
                MusicSettings.save();
                rebuildList();
            }).bounds(x, y, sideW(), 18).tooltip(Tooltip.create(tooltip)).build();
            addRenderableWidget(b);
            suggestionButtons.add(b);
            y += 20;
        }
    }

    private void renderSuggestionPanel(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int left = sideX() - 6;
        int right = sideX() + sideW() + 6;
        int top = TOP - 2;
        int bottom = list.getBottom() + 2;
        g.fill(left, top, right, bottom, PANEL_BG);
        g.outline(left - 1, top - 1, right - left + 2, bottom - top + 2, ACCENT_SOFT);
        g.fillGradient(left, top, right, top + 6, 0x30E89A3C, 0x00E89A3C);
        g.text(font, Lang.tr("playlist.suggest.title").withStyle(ChatFormatting.BOLD), sideX(), TOP + 2, GOLD);
        g.text(font, fit(Lang.tr("playlist.suggest.hint"), sideW()), sideX(), TOP + 12, MUTED);
        if (suggestionButtons.isEmpty()) g.text(font, fit(Lang.tr("playlist.suggest.empty"), sideW()), sideX(), TOP + 28, MUTED);
    }

    // ------------------------------------------------------------------ cizim

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        renderHeader(g);
        if (!addMode) renderNowPlaying(g);
        if (list.children().isEmpty()) {
            Component msg = addMode ? Lang.tr("screen.no_results") : Lang.tr(view == null ? "playlist.empty" : "playlist.list.empty");
            g.centeredText(font, msg, width / 2, TOP + 20, MUTED);
            if (!addMode) g.centeredText(font, Lang.tr(view == null ? "playlist.empty.hint" : "playlist.list.empty.hint"), width / 2, TOP + 34, MUTED);
        } else if (!addMode && ModOptions.gap == ModOptions.Gap.VANILLA) {
            // Ipucu: vanilla sarki arasi oyunda 5-20 dk
            g.centeredText(font, Lang.tr("playlist.gap_hint"), width / 2, height - 40, MUTED);
        }
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        Component title = Lang.tr("playlist.title").withStyle(ChatFormatting.BOLD);
        MutableComponent summary = Lang.count("tracks", viewSize()).append(" · ")
                .append(Lang.tr(ModOptions.playlistMode ? "playlist.mode_on" : "playlist.mode_off"));
        Playlist.SavedList playingList = Playlist.activeList();
        if (playingList != null && ModOptions.playlistMode) summary.append(" · ").append(Lang.tr("playlist.playing_list", playingList.name()));
        int logo = 22;
        int textW = Math.max(font.width(title), font.width(summary));
        int x = (width - (logo + 6 + textW)) / 2;
        int y = 4;
        QuietPleaseScreen.drawLogo(g, x, y, logo);
        g.text(font, title, x + logo + 6, y + 2, GOLD);
        g.text(font, summary, x + logo + 6, y + 13, ModOptions.playlistMode ? PLAYING : MUTED);
    }

    /** En ustte: calma listesinden calan sarki, ilerleme cubugu ve sure. */
    private void renderNowPlaying(GuiGraphicsExtractor g) {
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
            if (view != null) {
                Identifier next = Playlist.listNext(view);
                if (next == null) line = Lang.tr("playlist.list.bar.empty");
                else if (view != Playlist.activeList()) line = Lang.tr("playlist.list.bar.inactive");
                else if (!ModOptions.playlistMode) line = Lang.tr("playlist.bar.off");
                else line = Lang.tr("playlist.bar.next", Component.literal(shortName(next)).withStyle(s -> s.withColor(TEXT & 0xFFFFFF)));
            } else if (Playlist.isEmpty()) line = Lang.tr("playlist.bar.empty");
            else if (!ModOptions.playlistMode) line = Lang.tr("playlist.bar.off");
            else {
                List<Integer> order = Playlist.upcoming();
                Identifier next = order.isEmpty() ? Playlist.tracks().get(0) : Playlist.tracks().get(order.get(0));
                line = Lang.tr("playlist.bar.next", Component.literal(shortName(next)).withStyle(s -> s.withColor(TEXT & 0xFFFFFF)));
            }
            g.text(font, fit(line, right - left - 12), left + 8, top + 6, MUTED);
            return;
        }

        QuietPleaseScreen.drawEqualizer(g, left + 7, top + 12, PLAYING);
        int elapsed = Playlist.elapsedSeconds();
        int total = Playlist.durationSeconds(playing);
        String time = formatTime(elapsed) + (total > 0 ? " / " + formatTime(total) : "");
        int timeW = font.width(time);
        MutableComponent line = Component.literal(shortName(playing.trackId)).withStyle(s -> s.withColor(PLAYING & 0xFFFFFF))
                .append(Component.literal("  " + MusicLibrary.modName(playing.trackId.getNamespace())).withStyle(ChatFormatting.GRAY));
        g.text(font, fit(line, right - left - 30 - timeW - 8), left + 20, top + 3, TEXT);
        g.text(font, time, right - 4 - timeW, top + 3, MUTED);
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
        return Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(0, maxW - font.width("…"))) + "…")
                .withStyle(text.getStyle());
    }

    /** 75 -> "1:15" */
    private static String formatTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String shortName(Identifier id) {
        return MusicTrack.title(id);
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
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        // Mod filtresine sag tik: bir onceki mod (ana paneldeki gibi)
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && modButton != null && modButton.isMouseOver(mouseX, mouseY) && !modValues.isEmpty()) {
            int i = modValues.indexOf(modButton.getValue());
            String previous = modValues.get(Math.floorMod(i - 1, modValues.size()));
            modButton.setValue(previous);
            modFilter = previous;
            rebuildList();
            modButton.playDownSound(minecraft.getSoundManager());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

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
        MusicSettings.save();
    }

    // ------------------------------------------------------------------ liste

    private class RowList extends ContainerObjectSelectionList<Row> {
        RowList(Minecraft mc, int width, int height, int y) {
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

        @Override
        protected void extractListBackground(GuiGraphicsExtractor g) {
            int left = getRowLeft() - 6;
            int right = scrollBarX() + 6 + 4;
            int top = getY() - 2;
            int bottom = getBottom() + 2;
            g.fill(left, top, right, bottom, PANEL_BG);
            g.outline(left - 1, top - 1, right - left + 2, bottom - top + 2, ACCENT_SOFT);
            g.fillGradient(left, top, right, top + 6, 0x30E89A3C, 0x00E89A3C);
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor g) {}
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {}

    private static Button iconButton(String symbol, Component tooltip, Runnable action) {
        return Button.builder(Component.literal(symbol), b -> action.run()).size(18, 18).tooltip(Tooltip.create(tooltip)).build();
    }

    /** Listedeki bir sarki: numara, ad, mod, [▶] [▲] [▼] [∞] [✕]; kayitli listede dongu dugmesi yok. */
    private class QueueRow extends Row {
        private final int number;
        private final int index;
        private final Button play;
        private final Button up;
        private final Button down;
        private final Button remove;
        private final List<Button> buttons;

        QueueRow(int index) {
            this.number = index + 1;
            this.index = index;
            Playlist.SavedList saved = view;
            Identifier id = viewTracks().get(index);
            this.play = iconButton("▶", Lang.tr(saved == null ? "playlist.play_now.tooltip" : "playlist.list.play_now.tooltip"), () -> {
                if (saved == null) Playlist.playNow(index);
                else Playlist.startList(saved, index);
                MusicSettings.save();
                rebuildWidgets();
            });
            this.up = iconButton("▲", Lang.tr("playlist.up.tooltip"), () -> {
                if (saved == null) Playlist.swap(index, index - 1);
                else Playlist.listSwap(saved, index, index - 1);
                rebuildList();
            });
            this.down = iconButton("▼", Lang.tr("playlist.down.tooltip"), () -> {
                if (saved == null) Playlist.swap(index, index + 1);
                else Playlist.listSwap(saved, index, index + 1);
                rebuildList();
            });
            this.remove = iconButton("✕", Lang.tr(saved == null ? "playlist.remove.tooltip" : "playlist.list.remove.tooltip"), () -> {
                if (saved == null) Playlist.removeAt(index);
                else Playlist.listRemoveAt(saved, index);
                rebuildList();
            });
            up.active = index > 0;
            down.active = index < viewSize() - 1;
            if (saved == null) {
                boolean looping = Playlist.isLooping(id);
                Button loop = iconButton("∞", Lang.tr(looping ? "playlist.loop.on.tooltip" : "playlist.loop.off.tooltip"), () -> {
                    Playlist.toggleLoop(id);
                    MusicSettings.save();
                    rebuildList();
                });
                loop.setMessage(Component.literal("∞").withStyle(looping ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                this.buttons = List.of(play, up, down, loop, remove);
            } else {
                this.buttons = List.of(play, up, down, remove);
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int top = getContentY();
            int left = getContentX();
            int width = getContentWidth();
            int height = getContentHeight();
            if (index >= viewSize()) return; // liste degisti, yenilenmeyi bekliyor
            Identifier id = viewTracks().get(index);
            MusicTrack track = tracksById.get(id);
            boolean disabled = MusicSettings.isDisabled(id);
            boolean missing = track == null;
            boolean current = id.equals(viewCurrent());
            play.active = !disabled && !missing;

            if (hovering) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_HOVER);
            else if (current) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, PLAYING_BG);
            else if (number % 2 == 0) g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_ALT);
            // Soldaki cizgi: calan sarki yesil, imlecin ustundeki sarki turuncu
            if (current) g.fill(left - 3, top - 1, left - 1, top + height + 1, PLAYING);
            else if (hovering) g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);

            int textY = top + (height - 8) / 2;
            String num = number + ".";
            g.text(font, num, left + 18 - font.width(num), textY, MUTED);
            int nameX = left + 24;
            int buttonsX = left + width - buttons.size() * 20 + 2;
            int x = buttonsX;
            for (Button b : buttons) {
                b.setPosition(x, top + (height - 18) / 2);
                x += 20;
            }

            int color = current ? PLAYING : TEXT;
            MutableComponent name = Component.literal(shortName(id));
            if (disabled) name.withStyle(ChatFormatting.STRIKETHROUGH);
            MutableComponent info = Component.literal("  " + MusicLibrary.modName(id.getNamespace()));
            if (missing) info = Component.literal("  ").append(Lang.tr("playlist.missing")).withStyle(ChatFormatting.RED);
            else if (disabled) info.append(" · ").append(Lang.tr("screen.off"));
            MutableComponent line = name.withStyle(s -> s.withColor((disabled ? 0xFF6A6058 : color) & 0xFFFFFF))
                    .append(info.withStyle(missing ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
            g.text(font, fit(line, buttonsX - 6 - nameX), nameX, textY, color);

            for (Button b : buttons) b.extractRenderState(g, mouseX, mouseY, partialTick);
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
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int top = getContentY();
            int left = getContentX();
            int width = getContentWidth();
            int height = getContentHeight();
            g.fill(left - 3, top - 1, left + width + 3, top + height + 1, HEADER_BG);
            g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            Component name = Component.literal(modName).withStyle(ChatFormatting.BOLD);
            int y = top + (height - 8) / 2;
            g.text(font, name, left + 4, y, GOLD);
            g.text(font, Lang.count("tracks", count), left + 10 + font.width(name), y, MUTED);
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
        private final Button toggle;

        AddRow(MusicTrack track, boolean alternate) {
            this.track = track;
            this.alternate = alternate;
            this.icon = QuietPleaseScreen.iconFor(track);
            this.toggle = Button.builder(Component.empty(), b -> toggleInView(track.id()))
                    .size(70, 18).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int top = getContentY();
            int left = getContentX();
            int width = getContentWidth();
            int height = getContentHeight();
            boolean in = inView(track.id());
            if (hovering) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_HOVER);
                g.fill(left - 3, top - 1, left - 1, top + height + 1, ACCENT);
            } else if (alternate) {
                g.fill(left - 3, top - 1, left + width + 3, top + height + 1, ROW_ALT);
            }
            g.item(icon, left + 2, top + (height - 16) / 2);
            toggle.setPosition(left + width - toggle.getWidth(), top + (height - 18) / 2);
            toggle.setMessage(in ? Lang.tr("playlist.added").withStyle(ChatFormatting.GREEN) : Lang.tr("playlist.add"));
            String tip = in ? "added.tooltip" : "add.tooltip";
            toggle.setTooltip(Tooltip.create(Lang.tr(view == null ? "playlist." + tip : "playlist.list." + tip)));
            boolean disabled = MusicSettings.isDisabled(track.id());
            MutableComponent name = Component.literal(track.title());
            if (disabled) name.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY);
            MutableComponent line = name.append(Component.literal("  ").append(track.kind().displayName()).withStyle(ChatFormatting.DARK_GRAY));
            g.text(font, fit(line, toggle.getX() - 6 - (left + 22)), left + 22, top + (height - 8) / 2, in ? GOLD : TEXT);
            toggle.extractRenderState(g, mouseX, mouseY, partialTick);
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
