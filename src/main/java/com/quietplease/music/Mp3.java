package com.quietplease.music;

import com.quietplease.QuietPlease;
import com.quietplease.lib.javamp3.Sound;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

/**
 * Kendi muzik klasorundeki MP3 dosyalari. Minecraft yalnizca OGG cozer; MP3'ler moda gomulu JavaMP3 cozucusuyle
 * (com.quietplease.lib.javamp3) 16 bit PCM'e cevrilip ses motoruna akis olarak verilir (SoundBufferLibraryMixin).
 */
public final class Mp3 {

    private static final int[] BITRATE_L1 = {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448};
    private static final int[] BITRATE_L2 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384};
    private static final int[] BITRATE_L3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private static final int[] RATES = {44100, 48000, 32000};

    private Mp3() {}

    public static boolean isMp3(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    /** Dosyayi acar ve bastaki ID3v2 etiketlerini (kapak resmi dahil) atlar. */
    static InputStream open(Path file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
        try {
            while (true) {
                in.mark(10);
                byte[] h = in.readNBytes(10);
                if (h.length == 10 && h[0] == 'I' && h[1] == 'D' && h[2] == '3') {
                    long size = ((h[6] & 0x7FL) << 21) | ((h[7] & 0x7FL) << 14) | ((h[8] & 0x7FL) << 7) | (h[9] & 0x7FL);
                    if ((h[5] & 0x10) != 0) size += 10;
                    in.skipNBytes(size);
                } else {
                    in.reset();
                    return in;
                }
            }
        } catch (IOException | RuntimeException e) {
            in.close();
            throw e;
        }
    }

    public static AudioStream stream(Path file) throws IOException {
        InputStream in = open(file);
        try {
            return new Stream(new Sound(in));
        } catch (IOException | RuntimeException e) {
            in.close();
            throw e instanceof IOException io ? io : new IOException(e);
        }
    }

    public static AudioFormat format(Path file) throws IOException {
        try (InputStream in = open(file); Sound sound = new Sound(in)) {
            return sound.getAudioFormat();
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    /** Ses dengeleme olcumu icin tum dosyayi cozer: ornekler kanallar sirayla, -1..1 araliginda. */
    public static void decode(Path file, FloatConsumer out) throws IOException {
        try (InputStream in = open(file); Sound sound = new Sound(in)) {
            byte[] buf = new byte[1 << 15];
            int n;
            while ((n = sound.read(buf)) > 0) {
                for (int i = 0; i + 1 < n; i += 2) out.accept((short) ((buf[i] & 0xFF) | (buf[i + 1] << 8)) / 32768.0F);
            }
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Sure (saniye), cozmeden: kare basliklari tek tek gezilir, her kare 1152 (Layer I: 384) ornek. -1 = okunamadi.
     */
    public static int seconds(Path file) {
        try (InputStream in = open(file)) {
            byte[] b = in.readAllBytes();
            long samples = 0;
            int rate = 0;
            int i = 0;
            while (i + 4 <= b.length) {
                int h1 = b[i] & 0xFF, h2 = b[i + 1] & 0xFF, h3 = b[i + 2] & 0xFF;
                int layer = (h2 >> 1) & 3;
                int bitrateIndex = h3 >> 4;
                int rateIndex = (h3 >> 2) & 3;
                if (h1 != 0xFF || (h2 & 0xF8) != 0xF8 || layer == 0 || bitrateIndex == 0 || bitrateIndex == 15 || rateIndex == 3
                        || (rate != 0 && RATES[rateIndex] != rate)) {
                    i++;
                    continue;
                }
                rate = RATES[rateIndex];
                int padding = (h3 >> 1) & 1;
                int size;
                if (layer == 3) {
                    size = (12 * BITRATE_L1[bitrateIndex] * 1000 / rate + padding) * 4;
                    samples += 384;
                } else {
                    size = 144 * (layer == 2 ? BITRATE_L2 : BITRATE_L3)[bitrateIndex] * 1000 / rate + padding;
                    samples += 1152;
                }
                i += Math.max(size, 4);
            }
            return rate > 0 && samples > 0 ? (int) (samples / rate) : -1;
        } catch (IOException | RuntimeException e) {
            QuietPlease.LOGGER.debug("MP3 suresi okunamadi: {}", file, e);
            return -1;
        }
    }

    /** Ses motoruna verilen akis: her okumada istenen kadar PCM cozulur. */
    private static final class Stream implements AudioStream {
        private final Sound sound;
        private final AudioFormat format;
        private final int frameBytes;
        private byte[] buffer = new byte[0];
        private boolean ended;

        Stream(Sound sound) {
            this.sound = sound;
            this.format = sound.getAudioFormat();
            this.frameBytes = 2 * format.getChannels();
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            int want = Math.max(frameBytes, size - size % frameBytes);
            if (buffer.length < want) buffer = new byte[want];
            int n = 0;
            while (!ended && n < want) {
                int r;
                try {
                    r = sound.read(buffer, n, want - n);
                } catch (IOException | RuntimeException e) {
                    QuietPlease.LOGGER.warn("MP3 cozulurken hata, sarki burada bitiyor", e);
                    r = -1;
                }
                if (r < 0) ended = true;
                else n += r;
            }
            n -= n % frameBytes;
            ByteBuffer out = BufferUtils.createByteBuffer(n);
            out.put(buffer, 0, n);
            out.flip();
            return out;
        }

        @Override
        public void close() throws IOException {
            sound.close();
        }
    }
}
