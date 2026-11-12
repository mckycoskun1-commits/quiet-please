package com.quietplease;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.gui.QuietPleaseSettingsScreen;
import com.quietplease.gui.QuietPlaylistScreen;
import com.quietplease.music.MusicLibrary;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.MusicTrack;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Sohbet komutlari (sadece oyuncunun kendi bilgisayarinda calisir, sunucuya gitmez):
 *
 *   /quietplease                        modlara gore muzik sayilari (mod adina tiklayinca listesi acilir)
 *   /quietplease list <mod>             o modun parcalari, tiklanabilir [-] [+] [Kapat] dugmeleriyle
 *   /quietplease volume <parca> <0-100> parcanin ses seviyesi
 *   /quietplease off|on <parca>         parcayi kapat / ac
 *   /quietplease mod <mod> off|on       bir modun tum parcalarini kapat / ac
 *   /quietplease reset <parca>          parcayi varsayilana dondur
 *   /quietplease resetall               tum ayarlari sil
 *   /quietplease dump                   tam listeyi quietplease-tracks.txt dosyasina yazar
 *   /quietplease gui                    ayar panelini acar
 *   /quietplease settings               Ayarlar ekranini acar
 *   /quietplease playlist               calma listesi panelini acar
 */
public final class QuietPleaseCommands {

    private static final int VOLUME_STEP = 10;

    private static final SuggestionProvider<CommandSourceStack> MODS =
            (ctx, b) -> SharedSuggestionProvider.suggest(MusicLibrary.scan().keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> TRACKS =
            (ctx, b) -> SharedSuggestionProvider.suggestResource(
                    MusicLibrary.scan().values().stream().flatMap(List::stream).map(MusicTrack::id), b);

    private QuietPleaseCommands() {}

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("quietplease")
                .executes(QuietPleaseCommands::summary)
                .then(Commands.literal("list")
                        .then(Commands.argument("mod", StringArgumentType.word()).suggests(MODS)
                                .executes(ctx -> listMod(ctx, StringArgumentType.getString(ctx, "mod")))))
                .then(Commands.literal("volume")
                        .then(Commands.argument("track", ResourceLocationArgument.id()).suggests(TRACKS)
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> setVolume(ctx, track(ctx), IntegerArgumentType.getInteger(ctx, "percent"))))))
                .then(Commands.literal("off")
                        .then(Commands.argument("track", ResourceLocationArgument.id()).suggests(TRACKS)
                                .executes(ctx -> setEnabled(ctx, track(ctx), false))))
                .then(Commands.literal("on")
                        .then(Commands.argument("track", ResourceLocationArgument.id()).suggests(TRACKS)
                                .executes(ctx -> setEnabled(ctx, track(ctx), true))))
                .then(Commands.literal("mod")
                        .then(Commands.argument("mod", StringArgumentType.word()).suggests(MODS)
                                .then(Commands.literal("off").executes(ctx -> setModEnabled(ctx, false)))
                                .then(Commands.literal("on").executes(ctx -> setModEnabled(ctx, true)))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("track", ResourceLocationArgument.id()).suggests(TRACKS)
                                .executes(QuietPleaseCommands::reset)))
                .then(Commands.literal("resetall").executes(QuietPleaseCommands::resetAll))
                .then(Commands.literal("dump").executes(QuietPleaseCommands::dump))
                .then(Commands.literal("gui").executes(QuietPleaseCommands::openGui))
                .then(Commands.literal("settings").executes(ctx -> open(new QuietPleaseSettingsScreen(null))))
                .then(Commands.literal("playlist").executes(ctx -> open(new QuietPlaylistScreen(null)))));
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        return open(new QuietPleaseScreen(null));
    }

    private static int open(net.minecraft.client.gui.screens.Screen screen) {
        // Sohbet ekrani, komut baska bir ekran actiysa kendini kapatmaz (NeoForge duzeltmesi)
        Minecraft.getInstance().setScreen(screen);
        return 1;
    }

    private static ResourceLocation track(CommandContext<CommandSourceStack> ctx) {
        return ResourceLocationArgument.getId(ctx, "track");
    }

    // ------------------------------------------------------------------ listeleme

    private static int summary(CommandContext<CommandSourceStack> ctx) {
        Map<String, List<MusicTrack>> lib = MusicLibrary.scan();
        int total = lib.values().stream().mapToInt(List::size).sum();
        say(ctx, Lang.tr("cmd.summary", Lang.count("tracks", total), Lang.count("sources", lib.size()))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        lib.forEach((mod, tracks) -> {
            MutableComponent counts = Component.literal("  ");
            tracks.stream()
                    .collect(Collectors.groupingBy(MusicTrack::kind, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        if (!counts.getSiblings().isEmpty()) counts.append(", ");
                        counts.append(e.getValue() + " ").append(e.getKey().displayName());
                    });
            long off = tracks.stream().filter(t -> MusicSettings.isDisabled(t.id())).count();
            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(MusicLibrary.modName(mod)).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quietplease list " + mod))
                            .withHoverEvent(hover(Lang.tr("cmd.show_tracks")))))
                    .append(counts.withStyle(ChatFormatting.GRAY));
            if (off > 0) line.append(Component.literal(" (").append(Lang.tr("count.off", off)).append(")").withStyle(ChatFormatting.RED));
            say(ctx, line);
        });

        say(ctx, button(Lang.tr("cmd.open_panel"), "/quietplease gui", Lang.tr("cmd.open_panel.tooltip"), ChatFormatting.GREEN));
        say(ctx, Lang.tr("cmd.full_list").withStyle(ChatFormatting.GRAY)
                .append(button(Component.literal("/quietplease dump"), "/quietplease dump", Lang.tr("cmd.dump.tooltip"), ChatFormatting.YELLOW)));
        return total;
    }

    private static int listMod(CommandContext<CommandSourceStack> ctx, String mod) {
        List<MusicTrack> tracks = MusicLibrary.scan().get(mod);
        if (tracks == null) {
            ctx.getSource().sendFailure(Lang.tr("cmd.no_music", mod));
            return 0;
        }
        say(ctx, Component.literal("♪ " + MusicLibrary.modName(mod) + " (").append(Lang.count("tracks", tracks.size())).append(")  ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(button(Lang.tr("cmd.disable_all"), "/quietplease mod " + mod + " off", Lang.tr("cmd.disable_all.tooltip"), ChatFormatting.RED))
                .append(" ")
                .append(button(Lang.tr("cmd.enable_all"), "/quietplease mod " + mod + " on", Lang.tr("cmd.enable_all.tooltip"), ChatFormatting.GREEN)));
        for (MusicTrack t : tracks) say(ctx, trackLine(t));
        return tracks.size();
    }

    /** Bir parcanin satiri: tur, ad, ses seviyesi ve tiklanabilir dugmeler. */
    private static Component trackLine(MusicTrack t) {
        MusicSettings.TrackSetting s = MusicSettings.get(t.id());
        String id = t.id().toString();

        Component details = Component.literal(id).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("\n").append(Lang.tr("screen.events")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(t.events().stream().map(e -> "\n  " + e).sorted()
                        .collect(Collectors.joining())).withStyle(ChatFormatting.WHITE));

        MutableComponent line = Component.literal(" [").append(t.kind().displayName()).append("] ").withStyle(kindColor(t.kind()))
                .append(Component.literal(t.shortName()).withStyle(Style.EMPTY
                        .withColor(s.enabled() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)
                        .withStrikethrough(!s.enabled())
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, details))));

        if (s.enabled()) {
            line.append(Component.literal("  ").append(Lang.tr("cmd.volume", s.volume())).append(" ").withStyle(
                            s.volume() == MusicSettings.DEFAULT_VOLUME ? ChatFormatting.GRAY : ChatFormatting.YELLOW))
                    .append(button(Component.literal("[−]"), "/quietplease volume " + id + " " + Math.max(0, s.volume() - VOLUME_STEP),
                            Lang.tr("cmd.volume_down.tooltip", VOLUME_STEP), ChatFormatting.AQUA))
                    .append(button(Component.literal("[+]"), "/quietplease volume " + id + " " + Math.min(100, s.volume() + VOLUME_STEP),
                            Lang.tr("cmd.volume_up.tooltip", VOLUME_STEP), ChatFormatting.AQUA))
                    .append(" ")
                    .append(button(Lang.tr("cmd.turn_off"), "/quietplease off " + id, Lang.tr("cmd.turn_off.tooltip"), ChatFormatting.RED));
        } else {
            line.append(Component.literal("  ").append(Lang.tr("cmd.is_off")).append(" ").withStyle(ChatFormatting.RED))
                    .append(button(Lang.tr("cmd.turn_on"), "/quietplease on " + id, Lang.tr("cmd.turn_on.tooltip"), ChatFormatting.GREEN));
        }
        return line;
    }

    // ------------------------------------------------------------------ ayar degistirme

    private static int setVolume(CommandContext<CommandSourceStack> ctx, ResourceLocation id, int percent) {
        MusicSettings.setVolume(id, percent);
        return changed(ctx, id);
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, ResourceLocation id, boolean enabled) {
        MusicSettings.setEnabled(id, enabled);
        return changed(ctx, id);
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = track(ctx);
        MusicSettings.reset(id);
        return changed(ctx, id);
    }

    private static int setModEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        String mod = StringArgumentType.getString(ctx, "mod");
        List<MusicTrack> tracks = MusicLibrary.scan().get(mod);
        if (tracks == null) {
            ctx.getSource().sendFailure(Lang.tr("cmd.no_music", mod));
            return 0;
        }
        tracks.forEach(t -> MusicSettings.setEnabled(t.id(), enabled));
        MusicSettings.save();
        MusicSettings.applyToPlayingSounds();
        say(ctx, Lang.tr(enabled ? "cmd.mod_on" : "cmd.mod_off", MusicLibrary.modName(mod), Lang.count("tracks", tracks.size()))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        return tracks.size();
    }

    private static int resetAll(CommandContext<CommandSourceStack> ctx) {
        MusicSettings.resetAll();
        MusicSettings.save();
        MusicSettings.applyToPlayingSounds();
        say(ctx, Lang.tr("cmd.reset_all").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    /** Ayar degistikten sonra: kaydet, calan seslere uygula, guncel satiri goster. */
    private static int changed(CommandContext<CommandSourceStack> ctx, ResourceLocation id) {
        MusicSettings.save();
        MusicSettings.applyToPlayingSounds();
        MusicTrack track = MusicLibrary.scan().values().stream().flatMap(List::stream)
                .filter(t -> t.id().equals(id)).findFirst().orElse(null);
        if (track != null) {
            say(ctx, trackLine(track));
        } else {
            say(ctx, Lang.tr("cmd.not_loaded", id.toString())
                    .withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    // ------------------------------------------------------------------ dosyaya yazma

    private static int dump(CommandContext<CommandSourceStack> ctx) {
        Path file;
        try {
            file = writeDump();
        } catch (IOException e) {
            QuietPlease.LOGGER.error("Liste yazilamadi", e);
            ctx.getSource().sendFailure(Lang.tr("cmd.dump_failed", String.valueOf(e.getMessage())));
            return 0;
        }
        say(ctx, Lang.tr("cmd.dump_saved").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(file.getFileName().toString()).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.toAbsolutePath().toString())))));
        return 1;
    }

    /** Tum listeyi oyun klasorundeki quietplease-tracks.txt dosyasina yazar. */
    public static Path writeDump() throws IOException {
        Map<String, List<MusicTrack>> lib = MusicLibrary.scan();
        StringBuilder sb = new StringBuilder("Quiet Please - music tracks found\n\n");
        lib.forEach((mod, tracks) -> {
            sb.append("== ").append(MusicLibrary.modName(mod)).append(" (").append(mod).append(") - ")
                    .append(tracks.size()).append(" tracks ==\n");
            for (MusicTrack t : tracks) {
                MusicSettings.TrackSetting s = MusicSettings.get(t.id());
                String state = s.enabled() ? s.volume() + "%" : "OFF";
                sb.append(String.format("  [%-6s] %-7s %-45s <- %s%n", t.kind().id, state, t.id(),
                        t.events().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))));
            }
            sb.append('\n');
        });

        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("quietplease-tracks.txt");
        Files.writeString(file, sb, StandardCharsets.UTF_8);
        return file;
    }

    // ------------------------------------------------------------------ yardimcilar

    private static MutableComponent button(Component text, String command, Component tooltip, ChatFormatting color) {
        return text.copy().withStyle(Style.EMPTY.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(hover(tooltip)));
    }

    private static HoverEvent hover(Component text) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }

    private static ChatFormatting kindColor(MusicTrack.Kind kind) {
        return switch (kind) {
            case MUSIC -> ChatFormatting.GREEN;
            case DISC -> ChatFormatting.LIGHT_PURPLE;
            case OTHER -> ChatFormatting.DARK_AQUA;
        };
    }

    private static void say(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendSuccess(() -> msg, false);
    }
}
