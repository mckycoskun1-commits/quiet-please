package com.quietplease;

import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicControl;
import com.quietplease.music.Playlist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biomes;
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
    private static int phase = 0;
    private static long phaseStart;
    private static long cueTime = -1;
    private static int nextStep = 0;
    private static Process ffmpeg;

    private static volatile double camX, camY, camZ;
    private static float baseYaw;
    private static boolean camera = false;
    private static long camStart;
    private static float camYawSpeed = 4.5F;
    private static double camForward = 0.35;

    private static double curX = -1, curY = -1;
    private static double fromX, fromY, toX, toY;
    private static long glideStart, glideMs;
    private static boolean dragging = false;

    private record Step(long at, Runnable action) {}
    private static final List<Step> steps = new ArrayList<>();
    private static final List<String> marks = new ArrayList<>();

    public static void frame() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            run(mc);
        } catch (Exception e) {
            QuietPlease.LOGGER.error("PROMO error", e);
            phase = 99;
        }
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
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
                mc.options.guiScale().set(3);
                setupWindow(mc);
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.options.joinedFirstServer = true;
                var settings = new net.minecraft.world.level.LevelSettings("promo-" + System.currentTimeMillis(),
                        net.minecraft.world.level.GameType.CREATIVE,
                        new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false),
                        true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings,
                        new net.minecraft.world.level.levelgen.WorldOptions(1234L, false, false),
                        net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions, new TitleScreen());
                phase = 1;
            }
            case 1 -> {
                if (mc.player == null || mc.level == null || mc.screen != null) return;
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    var level = server.overworld();
                    var found = level.findClosestBiome3d(h -> h.is(Biomes.CHERRY_GROVE), mc.player.blockPosition(), 6400, 32, 64);
                    BlockPos p = found == null ? mc.player.blockPosition() : found.getFirst();
                    int top = p.getY();
                    for (int dx = -32; dx <= 32; dx += 4) {
                        for (int dz = -32; dz <= 32; dz += 4) {
                            int x = p.getX() + dx;
                            int z = p.getZ() + dz;
                            top = Math.max(top, level.getChunk(x >> 4, z >> 4)
                                    .getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1);
                        }
                    }
                    camX = p.getX() + 0.5;
                    camZ = p.getZ() + 0.5;
                    for (var sp : server.getPlayerList().getPlayers()) {
                        sp.getAbilities().flying = true;
                        sp.teleportTo(camX, top + 7, camZ);
                    }
                    camY = top + 7;
                    var src = server.createCommandSourceStack();
                    for (String c : List.of("time set 1500", "weather clear 1000000", "gamerule doDaylightCycle false",
                            "gamerule doWeatherCycle false", "gamerule advance_time false", "gamerule advance_weather false")) {
                        try {
                            server.getCommands().performPrefixedCommand(src, c);
                        } catch (Exception ignored) {
                        }
                    }
                    QuietPlease.LOGGER.info("PROMO location {} found={} camera y={}", p, found != null, camY);
                });
                phase = 2;
                phaseStart = now();
            }
            case 2 -> {
                if (camY == 0) return;
                mc.player.getAbilities().flying = true;
                mc.player.onUpdateAbilities();
                baseYaw = 0;
                camStart = now();
                camera = true;
                applyCamera(mc);
                phase = 3;
                phaseStart = now();
            }
            case 3 -> {
                applyCamera(mc);
                if (now() - phaseStart < 15000) return;
                mc.getToastManager().clear();
                mc.getMusicManager().stopPlaying();
                preparePlaylist();
                startFfmpeg();
                phase = 4;
                phaseStart = now();
            }
            case 4 -> {
                applyCamera(mc);
                if (now() - phaseStart < 3000) return;
                cueTime = now();
                camStart = cueTime;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 1.0F));
                mc.setScreen(new FlashScreen());
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
            default -> {
            }
        }
    }

    private static void setupWindow(Minecraft mc) {
        long h = mc.getWindow().handle();
        GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowPos(h, 0, 0);
        GLFW.glfwSetWindowSize(h, 1920, 1080);
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
        for (String s : List.of("nether/nether_wastes/rubedo", "left_to_bloom", "featherfall", "sweden")) {
            Playlist.add(Identifier.withDefaultNamespace("music/game/" + s));
        }
        com.quietplease.music.MusicSettings.setEnabled(Identifier.withDefaultNamespace("music/game/nether/nether_wastes/rubedo"), true);
        com.quietplease.music.MusicSettings.setVolume(Identifier.withDefaultNamespace("music/game/left_to_bloom"), 100);
    }

    private static void mark(String name) {
        marks.add(name + " " + (now() - cueTime));
    }

    private static void at(long ms, Runnable r) {
        steps.add(new Step(ms, r));
    }

    private static void buildTimeline(Minecraft mc) {
        at(80, () -> mc.setScreen(null));
        at(100, () -> {
            mc.options.hideGui = true;
            mark("scene1");
        });
        at(1500, () -> Playlist.playNow(0));

        at(8000, () -> {
            mark("scene2");
            camera = false;
            mc.options.hideGui = false;
            mc.setScreen(new QuietPleaseScreen(null));
            warp(mc, 320, 200);
        });
        at(8800, () -> glideTo(mc, find(mc, w -> w instanceof EditBox), 700));
        at(9600, () -> clickHere(mc));
        typeAt(9900, mc, "rubedo", 130);
        at(11300, () -> glideTo(mc, find(mc, PromoDirector::isToggle), 800));
        at(12300, () -> clickHere(mc));

        at(15500, () -> {
            mark("scene3");
            glideTo(mc, find(mc, w -> w instanceof EditBox), 600);
        });
        at(16200, () -> {
            clickHere(mc);
            EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
            if (box != null) box.setValue("");
        });
        typeAt(16500, mc, "left", 130);
        at(17600, () -> {
            AbstractWidget s = find(mc, w -> w.getClass().getSimpleName().equals("VolumeSlider"));
            if (s != null) glidePoint(mc, s.getX() + s.getWidth() - 4, s.getY() + s.getHeight() / 2.0, 700);
        });
        at(18500, () -> button(mc, 1));
        at(18600, () -> {
            AbstractWidget s = find(mc, w -> w.getClass().getSimpleName().equals("VolumeSlider"));
            if (s != null) {
                dragging = true;
                glidePoint(mc, s.getX() + 4 + 0.3 * (s.getWidth() - 8), s.getY() + s.getHeight() / 2.0, 1400);
            }
        });
        at(20100, () -> {
            dragging = false;
            button(mc, 0);
        });

        at(22500, () -> {
            mark("scene4");
            glideTo(mc, find(mc, w -> w instanceof EditBox), 500);
        });
        at(23100, () -> {
            clickHere(mc);
            EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
            if (box != null) box.setValue("");
        });
        at(23600, () -> glideTo(mc, find(mc, w -> w instanceof CycleButton<?> && w.getMessage().getString().startsWith("Mod")), 600));
        at(24300, () -> clickHere(mc));
        at(24900, () -> clickHere(mc));
        at(25600, () -> {
            AbstractWidget play = findText(mc, "▶");
            if (play != null) glidePoint(mc, play.getX() + 70, play.getY() + 10, 800);
        });
        at(27000, () -> {
            AbstractWidget play = findText(mc, "▶");
            if (play != null) glidePoint(mc, play.getX() + 70, play.getY() + 58, 1600);
        });

        long b = 30500;
        at(b, () -> {
            mark("scene5");
            glideTo(mc, findText(mc, "Playlist"), 700);
        });
        at(b + 800, () -> clickHere(mc));
        at(b + 2000, () -> glideTo(mc, findText(mc, "Add songs"), 700));
        at(b + 2800, () -> clickHere(mc));
        at(b + 3400, () -> glideTo(mc, find(mc, w -> w instanceof EditBox), 600));
        at(b + 4100, () -> clickHere(mc));
        typeAt(b + 4300, mc, "komorebi", 110);
        at(b + 5400, () -> glideTo(mc, findText(mc, "+ Add"), 700));
        at(b + 6200, () -> clickHere(mc));
        at(b + 6900, () -> {
            EditBox box = (EditBox) find(mc, w -> w instanceof EditBox);
            if (box != null) box.setValue("");
        });
        typeAt(b + 7000, mc, "aria", 120);
        at(b + 7700, () -> glideTo(mc, findText(mc, "+ Add"), 600));
        at(b + 8400, () -> clickHere(mc));
        at(b + 9100, () -> glideTo(mc, findText(mc, "Queue"), 700));
        at(b + 9900, () -> clickHere(mc));

        at(43000, () -> {
            mark("scene6");
            mc.setScreen(null);
            camera = true;
            camStart = now();
            baseYaw = 60;
        });
        at(44500, () -> {
            String result = MusicControl.skipAndDescribe();
            mc.gui.setOverlayMessage(Lang.tr(result).withStyle(net.minecraft.ChatFormatting.GREEN), false);
        });

        at(49500, () -> mark("scene7"));
        at(50000, () -> mc.gui.setOverlayMessage(QuietPleaseKeys.previous(), false));
        at(53500, () -> mc.gui.setOverlayMessage(QuietPleaseKeys.disablePlaying(mc), false));

        at(57000, () -> {
            mark("scene8");
            camera = false;
            mc.setScreen(new QuietPleaseScreen(null));
            warp(mc, 320, 200);
        });
        at(57800, () -> glideTo(mc, findText(mc, "Settings"), 700));
        at(58600, () -> clickHere(mc));
        at(59300, () -> glideTo(mc, findText(mc, "History & stats"), 800));
        at(60200, () -> clickHere(mc));
        at(62700, () -> glideTo(mc, findText(mc, "Most played"), 700));
        at(63500, () -> clickHere(mc));
        at(65500, () -> glideTo(mc, findText(mc, "Never played"), 700));
        at(66300, () -> clickHere(mc));

        at(68500, () -> {
            mark("scene9");
            mc.setScreen(null);
            mc.options.hideGui = true;
            camera = true;
            camStart = now();
            baseYaw = 150;
        });
        at(78500, () -> {
            mark("end");
            mc.options.hideGui = false;
        });
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

    private static AbstractWidget find(Minecraft mc, Predicate<AbstractWidget> p) {
        if (mc.screen == null) return null;
        List<AbstractWidget> out = new ArrayList<>();
        collect(mc.screen, out);
        int h = mc.getWindow().getGuiScaledHeight();
        return out.stream().filter(w -> w.visible && w.getY() > 2 && w.getY() < h - 2).filter(p)
                .min(Comparator.comparingInt(AbstractWidget::getY)).orElse(null);
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
        double s = (now() - camStart) / 1000.0;
        float yaw = baseYaw + (float) (s * camYawSpeed);
        double rad = Math.toRadians(yaw);
        double x = camX - Math.sin(rad) * s * camForward;
        double z = camZ + Math.cos(rad) * s * camForward;
        p.setPos(x, camY, z);
        p.xo = p.xOld = x;
        p.yo = p.yOld = camY;
        p.zo = p.zOld = z;
        p.setDeltaMovement(0, 0, 0);
        p.setYRot(yaw);
        p.setXRot(14.0F);
        p.yRotO = yaw;
        p.xRotO = 14.0F;
        p.setYHeadRot(yaw);
    }

    private static void startFfmpeg() throws Exception {
        Path out = Path.of(System.getenv("PROMO_OUT"));
        Files.createDirectories(out);
        ffmpeg = new ProcessBuilder(System.getenv("PROMO_FFMPEG"), "-y", "-f", "lavfi", "-i",
                "ddagrab=output_idx=0:framerate=60:draw_mouse=1", "-vf", "hwdownload,format=bgra",
                "-c:v", "h264_nvenc", "-preset", "p5", "-cq", "17", "-pix_fmt", "yuv420p",
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
