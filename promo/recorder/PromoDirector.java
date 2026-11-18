package com.quietplease;

import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicControl;
import com.quietplease.music.Playlist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.lwjgl.glfw.GLFW;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class PromoDirector {

    private PromoDirector() {}

    private static final boolean ENABLED = System.getenv("QUIETPLEASE_PROMO") != null;
    private static final boolean SCOUT = System.getenv("PROMO_SCOUT") != null;
    private static final boolean AUDIO_ONLY = System.getenv("PROMO_AUDIO_ONLY") != null;
    private static final boolean FIGURE_SCOUT = System.getenv("PROMO_FIGURES") != null;

    private static final long SEED = 1152036L;
    private static final double SHOT_X = -2768.5;
    private static final double SHOT_Y = 98.0;
    private static final double SHOT_Z = 2934.5;
    private static final float SHOT_YAW = 90.0F;
    private static final float SHOT_PITCH = 6.0F;
    private static final int SHOT_TIME = 12250;

    private static int[] SCOUT_TIMES = {12000, 12400, 12650};

    private static int phase = 0;
    private static long phaseStart;
    private static long cueTime = -1;
    private static int nextStep = 0;
    private static Process ffmpeg;

    private static volatile double camX, camY, camZ;
    private static volatile float camYaw = SHOT_YAW;
    private static volatile float camPitch = SHOT_PITCH;
    private static boolean camera = false;

    private static double curX = -1, curY = -1;
    private static double fromX, fromY, toX, toY;
    private static long glideStart, glideMs;
    private static boolean dragging = false;

    private record Step(long at, Runnable action) {}
    private static final List<Step> steps = new ArrayList<>();
    private static final List<String> marks = new ArrayList<>();

    private record Shot(double x, double y, double z, float yaw, float pitch, int score) {}
    private static final List<Shot> shots = new ArrayList<>();
    private static volatile boolean scoutReady = false;
    private static int shotIndex = 0;
    private static int timeIndex = 0;

    private static final double FIGURE_DIST_USED = 24.0;
    private static final double FIGURE_SIDE_USED = 0.0;
    private static final double CLOSE_BACK = 5.5;
    private static final double CLOSE_RIGHT = 1.6;
    private static final double CLOSE_LIFT = 0.6;
    private static final float CLOSE_PITCH = 4.0F;
    private static final int CLOSE_FOV = 45;
    private static final int WIDE_FOV = 70;
    private static volatile int camFov = WIDE_FOV;
    private static final Identifier BAD_SONG = Identifier.fromNamespaceAndPath("quietplease_music", "kazoo_disaster");
    private static final Identifier SWEDEN = Identifier.withDefaultNamespace("music/game/sweden");
    private static final long SWEDEN_LEAD_MS = 42000;
    private static final double[] FIGURE_DIST = {20.0, 22.0, 24.0};
    private static final double[] FIGURE_SIDE = {0.0, 3.0};
    private static final List<double[]> figures = new ArrayList<>();
    private static int figureIndex = 0;

    public static void frame() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            run(mc);
            if (phase >= 2 && phase != 7 && mc.player != null) cursorMode(mc);
        } catch (Exception e) {
            QuietPlease.LOGGER.error("PROMO error", e);
            phase = 99;
        }
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private static long seed() {
        String s = System.getenv("PROMO_SEED");
        return s == null ? SEED : Long.parseLong(s.trim());
    }

    private static void command(MinecraftServer server, String c) {
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        } catch (Exception ignored) {
        }
    }

    private static void worldRules(MinecraftServer server, int time) {
        for (String c : List.of("gamemode spectator @a", "time set " + time, "weather clear 1000000",
                "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                "gamerule advance_time false", "gamerule advance_weather false")) {
            command(server, c);
        }
    }

    private static void run(Minecraft mc) throws Exception {
        switch (phase) {
            case 0 -> {
                if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen(true));
                }
                if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1.0);
                for (SoundSource s : List.of(SoundSource.WEATHER, SoundSource.BLOCKS, SoundSource.HOSTILE, SoundSource.NEUTRAL,
                        SoundSource.PLAYERS, SoundSource.AMBIENT, SoundSource.VOICE)) {
                    mc.options.getSoundSourceOptionInstance(s).set(0.0);
                }
                mc.options.guiScale().set(3);
                mc.options.exclusiveFullscreen().set(false);
                mc.options.renderDistance().set(24);
                setupWindow(mc);
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.options.joinedFirstServer = true;
                var settings = new net.minecraft.world.level.LevelSettings("promo-" + System.currentTimeMillis(),
                        net.minecraft.world.level.GameType.CREATIVE,
                        new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false),
                        true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings,
                        new net.minecraft.world.level.levelgen.WorldOptions(seed(), false, false),
                        net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions, new TitleScreen());
                phase = 1;
            }
            case 1 -> {
                if (mc.player == null || mc.level == null || mc.screen != null) return;
                var server = mc.getSingleplayerServer();
                if (System.getenv("PROMO_SEEDSCAN") != null) {
                    server.execute(() -> {
                        try {
                            Files.write(Path.of(System.getenv("PROMO_OUT"), "seeds.txt"), scanSeeds(server.overworld()));
                        } catch (Exception e) {
                            QuietPlease.LOGGER.error("PROMO seed scan failed", e);
                        }
                        scoutReady = true;
                    });
                    phase = 30;
                    return;
                }
                if (SCOUT) {
                    BlockPos origin = mc.player.blockPosition();
                    String fixed = System.getenv("PROMO_SHOTS");
                    String times = System.getenv("PROMO_TIMES");
                    if (times != null) SCOUT_TIMES = java.util.Arrays.stream(times.split(",")).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
                    server.execute(() -> {
                        worldRules(server, SCOUT_TIMES[0]);
                        if (fixed != null) {
                            for (String part : fixed.split(";")) {
                                String[] v = part.trim().split(" ");
                                shots.add(new Shot(Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]),
                                        Float.parseFloat(v[3]), Float.parseFloat(v[4]), 0));
                            }                        } else {
                            findShots(server.overworld(), origin);
                        }
                        scoutReady = true;
                    });
                    mc.options.hideGui = true;
                    phase = 20;
                    return;
                }
                camX = SHOT_X;
                camY = SHOT_Y;
                camZ = SHOT_Z;
                if (FIGURE_SCOUT) {
                    server.execute(() -> {
                        worldRules(server, SHOT_TIME);
                        for (var sp : server.getPlayerList().getPlayers()) sp.teleportTo(SHOT_X, SHOT_Y, SHOT_Z);
                        findFigures(server.overworld());
                        if (!figures.isEmpty()) {
                            double[] f = figures.get(0);
                            command(server, "summon minecraft:mannequin " + f[0] + " " + f[1] + " " + f[2]
                                    + " {Rotation:[90f,0f],immovable:1b,hide_description:1b}");
                        }
                        scoutReady = true;
                    });
                    mc.options.hideGui = true;
                    mc.player.getAbilities().flying = true;
                    mc.player.onUpdateAbilities();
                    camera = true;
                    phase = 40;
                    phaseStart = now();
                    return;
                }
                server.execute(() -> {
                    worldRules(server, SHOT_TIME);
                    for (var sp : server.getPlayerList().getPlayers()) sp.teleportTo(SHOT_X, SHOT_Y, SHOT_Z);
                    QuietPlease.LOGGER.info("PROMO shot {} {} {} yaw {} pitch {} time {}", SHOT_X, SHOT_Y, SHOT_Z, SHOT_YAW, SHOT_PITCH, SHOT_TIME);
                    double[] f = figurePlace(server.overworld());
                    QuietPlease.LOGGER.info("PROMO figure at {} {} {} camera {} {} {}", f[0], f[1], f[2], f[3], f[4], f[5]);
                    command(server, "summon minecraft:mannequin " + f[0] + " " + f[1] + " " + f[2]
                            + " {Rotation:[90f,0f],immovable:1b,hide_description:1b}");
                    closeCamera(f);
                    clearPlants(server, f);
                    for (var sp : server.getPlayerList().getPlayers()) sp.teleportTo(f[3], f[4], f[5]);
                });
                phase = 2;
                phaseStart = now();
            }
            case 2 -> {
                mc.player.getAbilities().flying = true;
                mc.player.onUpdateAbilities();
                camera = true;
                applyCamera(mc);
                mc.getMusicManager().stopPlaying();
                preparePlaylist();
                Playlist.playNow(Playlist.tracks().indexOf(SWEDEN));
                phase = 3;
                phaseStart = now();
            }
            case 3 -> {
                applyCamera(mc);
                if (now() - phaseStart < SWEDEN_LEAD_MS) return;
                mc.getToastManager().clear();
                if (!AUDIO_ONLY) startFfmpeg();
                phase = 4;
                phaseStart = now();
            }
            case 4 -> {
                applyCamera(mc);
                if (now() - phaseStart < 3000) return;
                cueTime = now();
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 1.0F));
                mc.setScreen(new FlashScreen());
                mark("cue1");
                buildTimeline(mc);
                phase = 5;
            }
            case 5 -> {
                applyCamera(mc);
                applyGlide(mc);
                long t = now() - cueTime;
                while (nextStep < steps.size() && steps.get(nextStep).at() <= t) {
                    steps.get(nextStep).action().run();
                    nextStep++;
                }
                if (nextStep >= steps.size()) phase = 6;
            }
            case 6 -> {
                stopFfmpeg();
                Files.write(Path.of(System.getenv("PROMO_OUT"), "marks.txt"), marks);
                QuietPlease.LOGGER.info("PROMO done");
                phase = 7;
                mc.stop();
            }
            case 20 -> {
                if (!scoutReady) return;
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < shots.size(); i++) {
                    Shot s = shots.get(i);
                    lines.add(i + " " + s.x() + " " + s.y() + " " + s.z() + " yaw " + s.yaw() + " pitch " + s.pitch() + " score " + s.score());
                }
                Files.write(Path.of(System.getenv("PROMO_OUT"), "scout.txt"), lines);
                QuietPlease.LOGGER.info("PROMO scout found {} shots", shots.size());
                if (shots.isEmpty()) {
                    phase = 7;
                    mc.stop();
                    return;
                }
                shotIndex = 0;
                phase = 21;
            }
            case 21 -> {
                Shot s = shots.get(shotIndex);
                camX = s.x();
                camY = s.y();
                camZ = s.z();
                camYaw = s.yaw();
                camPitch = s.pitch();
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    command(server, "time set " + SCOUT_TIMES[0]);
                    for (var sp : server.getPlayerList().getPlayers()) sp.teleportTo(s.x(), s.y(), s.z());
                });
                mc.player.getAbilities().flying = true;
                camera = true;
                applyCamera(mc);
                phase = 22;
                phaseStart = now();
            }
            case 22 -> {
                applyCamera(mc);
                if (now() - phaseStart < (shotIndex == 0 || System.getenv("PROMO_SHOTS") == null ? 14000 : 6000)) return;
                timeIndex = 0;
                phase = 23;
                phaseStart = now();
            }
            case 23 -> {
                applyCamera(mc);
                if (now() - phaseStart < 2500) return;
                grab("shot" + shotIndex + "-t" + SCOUT_TIMES[timeIndex] + ".png");
                phase = 25;
                phaseStart = now();
            }
            case 25 -> {
                applyCamera(mc);
                if (now() - phaseStart < 1500) return;
                timeIndex++;
                if (timeIndex < SCOUT_TIMES.length) {
                    int time = SCOUT_TIMES[timeIndex];
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> command(server, "time set " + time));
                    phase = 23;
                    phaseStart = now();
                    return;
                }
                shotIndex++;
                phase = shotIndex < shots.size() ? 21 : 24;
                phaseStart = now();
            }
            case 24 -> {
                if (now() - phaseStart < 3000) return;
                phase = 7;
                mc.stop();
            }
            case 30 -> {
                if (!scoutReady) return;
                phase = 7;
                mc.stop();
            }
            case 40 -> {
                applyCamera(mc);
                if (!scoutReady || now() - phaseStart < 25000) return;
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < figures.size(); i++) {
                    double[] f = figures.get(i);
                    lines.add(i + " figure " + f[0] + " " + f[1] + " " + f[2] + " camera " + f[3] + " " + f[4] + " " + f[5] + " dist " + f[6] + " side " + f[7]);
                }
                Files.write(Path.of(System.getenv("PROMO_OUT"), "figures.txt"), lines);
                QuietPlease.LOGGER.info("PROMO figure scout: {} places", figures.size());
                figureIndex = 0;
                phase = figures.isEmpty() ? 24 : 41;
                phaseStart = now();
            }
            case 41 -> {
                applyCamera(mc);
                double[] f = figures.get(figureIndex);
                closeCamera(f);
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    clearPlants(server, f);
                    command(server, "tp @e[type=minecraft:mannequin] " + f[0] + " " + f[1] + " " + f[2] + " 90 0");
                    for (var sp : server.getPlayerList().getPlayers()) sp.teleportTo(f[3], f[4], f[5]);
                });
                phase = 42;
                phaseStart = now();
            }
            case 42 -> {
                applyCamera(mc);
                if (now() - phaseStart < 1800) return;
                grab("figure" + figureIndex + ".png");
                phase = 43;
                phaseStart = now();
            }
            case 43 -> {
                applyCamera(mc);
                if (now() - phaseStart < 900) return;
                figureIndex++;
                phase = figureIndex < figures.size() ? 41 : 24;
                phaseStart = now();
            }
            default -> {
            }
        }
    }

    private static boolean sea(ServerLevel level, int x, int z) {
        return level.getUncachedNoiseBiome(x >> 2, 15, z >> 2).is(BiomeTags.IS_OCEAN);
    }

    private static List<String> scanSeeds(ServerLevel level) {
        var gen = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = gen.generatorSettings().value();
        var noises = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.NOISE);
        var source = gen.getBiomeSource();
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 48; i++) {
            long seed = i * 104729L + 17;
            var sampler = net.minecraft.world.level.levelgen.RandomState.create(settings, noises, seed).sampler();
            java.util.function.BiPredicate<Integer, Integer> sea = (x, z) -> source.getNoiseBiome(x >> 2, 15, z >> 2, sampler).is(BiomeTags.IS_OCEAN);
            int count = 0;
            int[] best = null;
            for (int x = -3000; x <= 3000; x += 32) {
                for (int z = -3000; z <= 3000; z += 32) {
                    boolean c = false;
                    for (int qy : new int[]{18, 21, 25, 30}) {
                        if (source.getNoiseBiome(x >> 2, qy, z >> 2, sampler).is(Biomes.CHERRY_GROVE)) {
                            c = true;
                            break;
                        }
                    }
                    if (!c) continue;
                    int d = -1;
                    for (int k = 8; k <= 120; k += 8) {
                        if (sea.test(x - k, z)) {
                            d = k;
                            break;
                        }
                    }
                    if (d < 24) continue;
                    int sx = x - d;
                    if (!(sea.test(sx - 48, z) && sea.test(sx - 128, z) && sea.test(sx - 96, z - 48) && sea.test(sx - 96, z + 48))) continue;
                    count++;
                    if (best == null || Math.abs(d - 60) < Math.abs(best[2] - 60)) best = new int[]{x, z, d};
                }
            }
            String line = seed + " candidates " + count + (best == null ? "" : " best " + best[0] + " " + best[1] + " sea at " + best[2]);
            QuietPlease.LOGGER.info("PROMO seed {}", line);
            out.add(line);
        }
        return out;
    }

    private static double[] figureAt(ServerLevel level, double dist, double side) {
        double x = SHOT_X - dist;
        double z = SHOT_Z - side;
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int y = topY(level, bx, bz);
        BlockState below = level.getBlockState(new BlockPos(bx, y - 1, bz));
        double cx = x + CLOSE_BACK;
        double cz = z + CLOSE_RIGHT;
        double cy = Math.max(y + CLOSE_LIFT, topY(level, (int) Math.floor(cx), (int) Math.floor(cz)) + 0.3);
        boolean ok = below.getFluidState().isEmpty() && !below.is(Blocks.CHERRY_LOG) && !below.is(Blocks.CHERRY_LEAVES);
        if (!ok) QuietPlease.LOGGER.info("PROMO figure skip {} {} on {}", x, z, below);
        return ok ? new double[]{x, y, z, cx, cy, cz, dist, side} : null;
    }

    private static void findFigures(ServerLevel level) {
        for (double dist : FIGURE_DIST) {
            for (double side : FIGURE_SIDE) {
                double[] f = figureAt(level, dist, side);
                if (f != null) figures.add(f);
            }
        }
    }

    private static void closeCamera(double[] f) {
        camX = f[3];
        camY = f[4];
        camZ = f[5];
        camYaw = SHOT_YAW;
        camPitch = CLOSE_PITCH;
        camFov = CLOSE_FOV;
    }

    private static void clearPlants(MinecraftServer server, double[] f) {
        int x0 = (int) Math.floor(f[0]) - 3;
        int x1 = (int) Math.floor(f[3]) + 1;
        int y0 = (int) Math.floor(f[1]) - 1;
        int y1 = (int) Math.floor(f[4]) + 3;
        int z0 = (int) Math.floor(Math.min(f[2], f[5])) - 2;
        int z1 = (int) Math.floor(Math.max(f[2], f[5])) + 2;
        for (String b : List.of("short_grass", "tall_grass", "fern", "large_fern", "bush", "short_dry_grass", "tall_dry_grass", "dead_bush")) {
            command(server, "fill " + x0 + " " + y0 + " " + z0 + " " + x1 + " " + y1 + " " + z1 + " minecraft:air replace minecraft:" + b);
        }
    }

    private static void wideCamera() {
        camX = SHOT_X;
        camY = SHOT_Y;
        camZ = SHOT_Z;
        camYaw = SHOT_YAW;
        camPitch = SHOT_PITCH;
        camFov = WIDE_FOV;
    }

    private static boolean cherry(ServerLevel level, int x, int z) {
        for (int qy : new int[]{18, 21, 25, 30}) {
            if (level.getUncachedNoiseBiome(x >> 2, qy, z >> 2).is(Biomes.CHERRY_GROVE)) return true;
        }
        return false;
    }

    private static int topY(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
    }

    private static void findShots(ServerLevel level, BlockPos origin) {
        List<int[]> found = new ArrayList<>();
        int ox = origin.getX();
        int oz = origin.getZ();
        int cherry = 0;
        int near = 0;
        for (int x = ox - 7000; x <= ox + 7000; x += 24) {
            for (int z = oz - 7000; z <= oz + 7000; z += 24) {
                if (!cherry(level, x, z)) continue;
                cherry++;
                int d = -1;
                for (int k = 8; k <= 400; k += 8) {
                    if (sea(level, x - k, z)) {
                        d = k;
                        break;
                    }
                }
                if (d < 0) continue;
                near++;
                int sx = x - d;
                if (!(sea(level, sx - 48, z) && sea(level, sx - 128, z) && sea(level, sx - 96, z - 48) && sea(level, sx - 96, z + 48))) continue;
                found.add(new int[]{x, z, d});
            }
        }
        QuietPlease.LOGGER.info("PROMO scout cherry samples {} sea within 400 west {} coast candidates {}", cherry, near, found.size());
        found.sort(Comparator.comparingDouble(a -> Math.abs(a[2] - 60) + Math.hypot(a[0] - ox, a[1] - oz) / 100.0));
        for (int[] f : found) QuietPlease.LOGGER.info("PROMO scout coast {} {} sea at {}", f[0], f[1], f[2]);
        List<int[]> picked = new ArrayList<>();
        for (int[] f : found) {
            if (picked.stream().anyMatch(p -> Math.hypot(p[0] - f[0], p[1] - f[1]) < 120)) continue;
            picked.add(f);
            if (picked.size() >= 5) break;
        }
        for (int[] f : picked) {
            Shot s = placeCamera(level, f[0], f[1]);
            QuietPlease.LOGGER.info("PROMO scout candidate {} {} -> {}", f[0], f[1], s);
            if (s != null) shots.add(s);
        }
    }

    private static Shot placeCamera(ServerLevel level, int x, int z) {
        int shore = Integer.MIN_VALUE;
        for (int k = 0; k <= 420; k++) {
            int cx = x - k;
            int h = topY(level, cx, z);
            if (h <= 63 && level.getBlockState(new BlockPos(cx, h, z)).getFluidState().is(FluidTags.WATER)) {
                shore = cx;
                break;
            }
        }
        if (shore == Integer.MIN_VALUE) return null;
        Shot best = null;
        for (int back = 6; back <= x - shore + 24; back += 3) {
            for (int dz = -15; dz <= 15; dz += 3) {
                int cx = shore + back;
                int cz = z + dz;
                int g = topY(level, cx, cz);
                if (g < 63 || !level.getBlockState(new BlockPos(cx, g, cz)).getFluidState().isEmpty()) continue;
                for (double lift : new double[]{1.0, 2.5, 5.0}) {
                    double ex = cx + 0.5;
                    double ey = g + 1 + lift + 1.62;
                    double ez = cz + 0.5;
                    if (!level.getBlockState(BlockPos.containing(ex, ey, ez)).isAir()) continue;
                    if (!clear(level, ex, ey, ez, SHOT_YAW, 0, 30)) continue;
                    if (!clear(level, ex, ey, ez, SHOT_YAW, 4, 20)) continue;
                    if (!clear(level, ex, ey, ez, SHOT_YAW - 15, 2, 14)) continue;
                    if (!clear(level, ex, ey, ez, SHOT_YAW + 15, 2, 14)) continue;
                    int left = leaves(level, ex, ey, ez, SHOT_YAW - 34) + leaves(level, ex, ey, ez, SHOT_YAW - 44);
                    int right = leaves(level, ex, ey, ez, SHOT_YAW + 34) + leaves(level, ex, ey, ez, SHOT_YAW + 44);
                    int score = Math.min(left, 4) + Math.min(right, 4) + (left > 0 && right > 0 ? 6 : 0) - back / 40;
                    if (best == null || score > best.score()) best = new Shot(ex, ey - 1.62, ez, SHOT_YAW, SHOT_PITCH, score);
                }
            }
        }
        return best;
    }

    private static double[] dir(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new double[]{-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    private static boolean clear(ServerLevel level, double x, double y, double z, float yaw, float pitch, double dist) {
        double[] d = dir(yaw, pitch);
        for (double s = 0.5; s <= dist; s += 0.5) {
            BlockState st = level.getBlockState(BlockPos.containing(x + d[0] * s, y + d[1] * s, z + d[2] * s));
            if (!st.isAir() && st.getFluidState().isEmpty()) return false;
        }
        return true;
    }

    private static int leaves(ServerLevel level, double x, double y, double z, float yaw) {
        int n = 0;
        for (float pitch : new float[]{-6.0F, 4.0F}) {
            double[] d = dir(yaw, pitch);
            for (double s = 3; s <= 16; s += 1) {
                if (level.getBlockState(BlockPos.containing(x + d[0] * s, y + d[1] * s, z + d[2] * s)).is(Blocks.CHERRY_LEAVES)) n++;
            }
        }
        return n;
    }

    private static void grab(String name) {
        try {
            Path out = Path.of(System.getenv("PROMO_OUT"));
            Files.createDirectories(out);
            new ProcessBuilder(System.getenv("PROMO_FFMPEG"), "-y", "-loglevel", "error", "-f", "lavfi", "-i",
                    "ddagrab=output_idx=0:framerate=10", "-vf", "hwdownload,format=bgra", "-frames:v", "1", out.resolve(name).toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (Exception e) {
            QuietPlease.LOGGER.error("PROMO grab failed", e);
        }
    }

    private static void setupWindow(Minecraft mc) {
        long h = mc.getWindow().handle();
        GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowPos(h, 0, 0);
        GLFW.glfwSetWindowSize(h, 1920, 1080);
        GLFW.glfwFocusWindow(h);
        GLFW.glfwSetCursorPos(h, 960, 540);
    }

    private static void cursorMode(Minecraft mc) {
        long h = mc.getWindow().handle();
        int mode = GLFW.glfwGetInputMode(h, GLFW.GLFW_CURSOR);
        if (mc.screen == null && mode == GLFW.GLFW_CURSOR_NORMAL) {
            GLFW.glfwSetCursorPos(h, 960, 540);
            GLFW.glfwSetInputMode(h, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else if (mc.screen != null && mode == GLFW.GLFW_CURSOR_HIDDEN) {
            GLFW.glfwSetInputMode(h, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    private static void preparePlaylist() {
        ModOptions.playlistMode = true;
        ModOptions.gap = ModOptions.Gap.NONE;
        ModOptions.fadeOnSkip = true;
        ModOptions.normalize = false;
        ModOptions.popupPosition = ModOptions.PopupPosition.RIGHT_MIDDLE;
        com.quietplease.music.MusicSettings.resetAll();
        com.quietplease.music.ListenStats.clear();
        for (Identifier fav : new ArrayList<>(Playlist.favorites())) Playlist.toggleFavorite(fav);
        Playlist.clear();
        com.quietplease.music.CustomMusic.refresh();
        Playlist.add(BAD_SONG);
        for (String s : List.of("left_to_bloom", "featherfall", "sweden")) {
            Playlist.add(Identifier.withDefaultNamespace("music/game/" + s));
        }
        com.quietplease.music.MusicSettings.setEnabled(BAD_SONG, true);
        com.quietplease.music.MusicSettings.setVolume(Identifier.withDefaultNamespace("music/game/left_to_bloom"), 100);
    }

    private static double[] figurePlace(ServerLevel level) {
        double dist = FIGURE_DIST_USED;
        double side = FIGURE_SIDE_USED;
        String env = System.getenv("PROMO_FIGURE");
        if (env != null) {
            String[] v = env.trim().split(" ");
            dist = Double.parseDouble(v[0]);
            side = Double.parseDouble(v[1]);
        }
        double[] f = figureAt(level, dist, side);
        if (f != null) return f;
        double x = SHOT_X - dist;
        double z = SHOT_Z - side;
        double y = topY(level, (int) Math.floor(x), (int) Math.floor(z));
        return new double[]{x, y, z, x + CLOSE_BACK, y + CLOSE_LIFT, z + CLOSE_RIGHT, dist, side};
    }

    private static void figure(Minecraft mc, String command) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> command(server, command));
    }

    private static void facing(Minecraft mc, float yaw) {
        figure(mc, "execute as @e[type=minecraft:mannequin] at @s run tp @s ~ ~ ~ " + yaw + " 0");
    }

    private static void pose(Minecraft mc, String pose) {
        figure(mc, "data merge entity @e[type=minecraft:mannequin,limit=1] {pose:\"" + pose + "\"}");
    }

    private static void mark(String name) {
        marks.add(name + " " + (now() - cueTime));
    }

    private static void markRect(Minecraft mc, String name, int x, int y, int w, int h) {
        double s = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
        marks.add(name + " " + (now() - cueTime) + " " + Math.round(x * s) + " " + Math.round(y * s)
                + " " + Math.round(w * s) + " " + Math.round(h * s));
    }

    private static void markWidgets(Minecraft mc, String name, List<AbstractWidget> ws, int pad) {
        if (ws.isEmpty()) {
            QuietPlease.LOGGER.warn("PROMO zoom target missing: {}", name);
            return;
        }
        int x0 = ws.stream().mapToInt(AbstractWidget::getX).min().getAsInt() - pad;
        int y0 = ws.stream().mapToInt(AbstractWidget::getY).min().getAsInt() - pad;
        int x1 = ws.stream().mapToInt(w -> w.getX() + w.getWidth()).max().getAsInt() + pad;
        int y1 = ws.stream().mapToInt(w -> w.getY() + w.getHeight()).max().getAsInt() + pad;
        markRect(mc, name, x0, y0, x1 - x0, y1 - y0);
    }

    private static void cue(Minecraft mc, String name, boolean pling) {
        mc.options.hideGui = false;
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                pling ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.UI_BUTTON_CLICK.value(), pling ? 2.0F : 1.0F, 1.0F));
        mc.setScreen(new FlashScreen());
        mark(name);
    }

    private static void mute(Minecraft mc) {
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
    }

    private static void at(long ms, Runnable r) {
        steps.add(new Step(ms, r));
    }

    private static void buildTimeline(Minecraft mc) {
        at(80, () -> mc.setScreen(null));
        at(100, () -> {
            mc.options.hideGui = true;
            mark("sceneA");
        });
        at(6500, () -> mark("sceneE"));
        at(12000, () -> mark("sceneF"));

        at(20500, () -> {
            mark("sceneB");
            Playlist.playNow(Playlist.tracks().indexOf(BAD_SONG));
        });
        at(21000, () -> facing(mc, 270.0F));
        for (int i = 0; i < 14; i++) {
            String p = i % 2 == 0 ? "crouching" : "standing";
            at(21500 + i * 180L, () -> pose(mc, p));
        }

        at(26300, () -> mute(mc));
        at(27000, () -> cue(mc, "cue2", false));
        at(27080, () -> {
            mc.setScreen(null);
            mc.options.hideGui = true;
            facing(mc, 90.0F);
            wideCamera();
        });

        long d = 28000;
        at(d, () -> {
            mark("sceneD1");
            mc.options.hideGui = false;
            mc.setScreen(new QuietPleaseScreen(null));
            warp(mc, 320, 200);
        });
        at(d + 300, () -> glideTo(mc, find(mc, w -> w instanceof EditBox), 450));
        at(d + 800, () -> clickHere(mc));
        typeAt(d + 950, mc, "kazoo", 80);
        at(d + 1500, () -> glideTo(mc, find(mc, PromoDirector::isToggle), 500));
        at(d + 2100, () -> {
            clickHere(mc);
            mark("off");
        });

        at(d + 3300, () -> {
            mark("sceneD2");
            glideTo(mc, find(mc, w -> w instanceof EditBox), 350);
        });
        at(d + 3700, () -> {
            clickHere(mc);
            EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
            if (box != null) box.setValue("");
        });
        typeAt(d + 3800, mc, "left", 80);
        at(d + 4300, () -> {
            AbstractWidget s = find(mc, w -> w.getClass().getSimpleName().equals("VolumeSlider"));
            if (s == null) return;
            glidePoint(mc, s.getX() + s.getWidth() - 4, s.getY() + s.getHeight() / 2.0, 450);
            markWidgets(mc, "zoom3", all(mc, w -> Math.abs(w.getY() - s.getY()) <= 2), 4);
        });
        at(d + 4800, () -> button(mc, 1));
        at(d + 4850, () -> {
            AbstractWidget s = find(mc, w -> w.getClass().getSimpleName().equals("VolumeSlider"));
            if (s != null) {
                dragging = true;
                glidePoint(mc, s.getX() + 4 + 0.3 * (s.getWidth() - 8), s.getY() + s.getHeight() / 2.0, 900);
            }
        });
        at(d + 5800, () -> {
            dragging = false;
            button(mc, 0);
        });

        at(d + 6600, () -> {
            mark("sceneD3");
            glideTo(mc, find(mc, w -> w instanceof EditBox), 350);
        });
        at(d + 7000, () -> {
            clickHere(mc);
            EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
            if (box != null) box.setValue("");
        });
        at(d + 7150, () -> glideTo(mc, find(mc, w -> w instanceof CycleButton<?> && w.getMessage().getString().startsWith("Mod")), 400));
        at(d + 7600, () -> clickHere(mc));
        at(d + 7900, () -> clickHere(mc));
        at(d + 8300, () -> glideTo(mc, findText(mc, "▶"), 450));
        at(d + 8800, () -> {
            clickHere(mc);
            mark("play");
        });
        at(d + 8900, () -> glidePoint(mc, 320, 210, 500));
        at(d + 10300, () -> glideTo(mc, findText(mc, "■"), 400));
        at(d + 10750, () -> clickHere(mc));
        at(d + 11000, () -> {
            mark("sceneD3b");
            glideTo(mc, findText(mc, "Playlist"), 450);
        });
        at(d + 11500, () -> clickHere(mc));
        at(d + 12000, () -> glideTo(mc, findText(mc, "Add songs"), 400));
        at(d + 12450, () -> clickHere(mc));
        at(d + 12800, () -> glideTo(mc, find(mc, w -> w instanceof EditBox), 350));
        at(d + 13200, () -> clickHere(mc));
        typeAt(d + 13300, mc, "aria", 80);
        at(d + 13800, () -> glideTo(mc, findText(mc, "+ Add"), 400));
        at(d + 14250, () -> clickHere(mc));
        at(d + 14400, () -> glidePoint(mc, 300, 220, 300));
        at(d + 14800, () -> glideTo(mc, findText(mc, "Queue"), 400));
        at(d + 15250, () -> clickHere(mc));

        at(d + 16600, () -> mute(mc));
        at(d + 17300, () -> cue(mc, "cue3", false));
        at(d + 17380, () -> mc.setScreen(null));

        at(d + 18000, () -> {
            mark("sceneD4");
            mc.setScreen(null);
        });
        at(d + 18500, () -> {
            String result = MusicControl.skipAndDescribe();
            mc.gui.setOverlayMessage(Lang.tr(result).withStyle(net.minecraft.ChatFormatting.GREEN), false);
            mark("skip");
        });
        at(d + 20300, () -> {
            mc.gui.setOverlayMessage(QuietPleaseKeys.previous(), false);
            mark("previous");
        });
        at(d + 22000, () -> {
            mc.gui.setOverlayMessage(QuietPleaseKeys.disablePlaying(mc), false);
            mark("never");
        });

        at(d + 24200, () -> {
            mark("sceneD5");
            mc.setScreen(new QuietPleaseScreen(null));
            warp(mc, 320, 200);
        });
        at(d + 24300, () -> glideTo(mc, findText(mc, "Settings"), 400));
        at(d + 24750, () -> clickHere(mc));
        at(d + 24900, () -> glideTo(mc, findText(mc, "History & stats"), 450));
        at(d + 25400, () -> clickHere(mc));
        at(d + 26300, () -> glideTo(mc, findText(mc, "Most played"), 400));
        at(d + 26750, () -> clickHere(mc));
        at(d + 26850, () -> glidePoint(mc, 320, mc.getWindow().getGuiScaledHeight() - 60, 400));
        at(d + 26950, () -> {
            int h = mc.getWindow().getGuiScaledHeight();
            markWidgets(mc, "zoom8", all(mc, w -> w.getY() >= 28 && w.getY() < h - 36), 4);
        });

        at(d + 29500, () -> {
            mark("end");
            mc.setScreen(null);
            mc.options.hideGui = true;
        });
        at(d + 29600, () -> mute(mc));
        at(d + 31200, () -> cue(mc, "cue4", true));
        at(d + 31300, () -> mc.setScreen(null));
        at(d + 32200, () -> mark("stop"));
    }

    private static void typeAt(long start, Minecraft mc, String text, long perChar) {
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            at(start + i * perChar, () -> {
                if (mc.screen != null && mc.screen.getFocused() instanceof EditBox box) box.insertText(ch);
                else {
                    EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
                    if (box != null) box.insertText(ch);
                }
            });
        }
    }

    private static boolean isToggle(AbstractWidget w) {
        String s = w.getMessage().getString();
        return s.equals("On") || s.equals("Off");
    }

    private static AbstractWidget findText(Minecraft mc, String prefix) {
        return find(mc, w -> w.getMessage().getString().startsWith(prefix));
    }

    private static List<AbstractWidget> all(Minecraft mc, Predicate<AbstractWidget> p) {
        if (mc.screen == null) return List.of();
        List<AbstractWidget> out = new ArrayList<>();
        collect(mc.screen, out);
        int h = mc.getWindow().getGuiScaledHeight();
        return out.stream().filter(w -> w.visible && w.getY() > 2 && w.getY() < h - 2).filter(p).toList();
    }

    private static AbstractWidget find(Minecraft mc, Predicate<AbstractWidget> p) {
        return all(mc, p).stream().min(Comparator.comparingInt(AbstractWidget::getY)).orElse(null);
    }

    private static void collect(GuiEventListener l, List<AbstractWidget> out) {
        if (l instanceof AbstractWidget w) out.add(w);
        if (l instanceof ContainerEventHandler c) {
            for (GuiEventListener child : c.children()) collect(child, out);
        }
    }

    private static void glideTo(Minecraft mc, AbstractWidget w, long ms) {
        if (w == null) {
            QuietPlease.LOGGER.warn("PROMO target missing");
            return;
        }
        glidePoint(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0, ms);
    }

    private static void glidePoint(Minecraft mc, double x, double y, long ms) {
        if (curX < 0) warp(mc, x, y);
        fromX = curX;
        fromY = curY;
        toX = x;
        toY = y;
        glideStart = now();
        glideMs = ms;
    }

    private static void warp(Minecraft mc, double x, double y) {
        curX = fromX = toX = x;
        curY = fromY = toY = y;
        glideMs = 0;
        moveCursor(mc, x, y);
    }

    private static void applyGlide(Minecraft mc) {
        if (glideMs <= 0 || mc.screen == null) return;
        double f = Math.min(1.0, (now() - glideStart) / (double) glideMs);
        double e = f * f * (3 - 2 * f);
        curX = fromX + (toX - fromX) * e;
        curY = fromY + (toY - fromY) * e;
        moveCursor(mc, curX, curY);
        if (f >= 1.0) glideMs = 0;
    }

    private static void moveCursor(Minecraft mc, double guiX, double guiY) {
        var window = mc.getWindow();
        double px = guiX * window.getScreenWidth() / window.getGuiScaledWidth();
        double py = guiY * window.getScreenHeight() / window.getGuiScaledHeight();
        GLFW.glfwSetCursorPos(window.handle(), px, py);
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, window.handle(), px, py);
        } catch (ReflectiveOperationException e) {
            QuietPlease.LOGGER.error("PROMO move failed", e);
        }
    }

    private static void clickHere(Minecraft mc) {
        button(mc, 1);
        button(mc, 0);
    }

    private static void button(Minecraft mc, int action) {
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onButton", long.class,
                    net.minecraft.client.input.MouseButtonInfo.class, int.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, mc.getWindow().handle(),
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), action);
        } catch (ReflectiveOperationException e) {
            QuietPlease.LOGGER.error("PROMO click failed", e);
        }
    }

    private static void applyCamera(Minecraft mc) {
        var p = mc.player;
        if (p == null || !camera) return;
        p.setPos(camX, camY, camZ);
        p.xo = p.xOld = camX;
        p.yo = p.yOld = camY;
        p.zo = p.zOld = camZ;
        p.setDeltaMovement(0, 0, 0);
        p.setYRot(camYaw);
        p.setXRot(camPitch);
        p.yRotO = camYaw;
        p.xRotO = camPitch;
        p.setYHeadRot(camYaw);
        if (mc.options.fov().get() != camFov) mc.options.fov().set(camFov);
    }

    private static void startFfmpeg() throws Exception {
        Path out = Path.of(System.getenv("PROMO_OUT"));
        Files.createDirectories(out);
        ffmpeg = new ProcessBuilder(System.getenv("PROMO_FFMPEG"), "-y", "-f", "lavfi", "-i",
                "ddagrab=output_idx=0:framerate=60:draw_mouse=1",
                "-c:v", "h264_nvenc", "-preset", "p5", "-cq", "17",
                out.resolve("raw.mp4").toString())
                .redirectErrorStream(true)
                .redirectOutput(out.resolve("ffmpeg.log").toFile())
                .start();
    }

    private static void stopFfmpeg() throws Exception {
        if (ffmpeg == null) return;
        OutputStream in = ffmpeg.getOutputStream();
        in.write('q');
        in.flush();
        ffmpeg.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static final class FlashScreen extends Screen {
        FlashScreen() {
            super(Component.empty());
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFFFFFFFF);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        }
    }
}
