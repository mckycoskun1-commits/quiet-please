import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class IconRender {
    static final int SS = 4;
    static final double SQ = Math.sqrt(0.5);
    static final double PITCH = Math.toRadians(30);
    static final double[] R = {SQ, 0, -SQ};
    static final double[] F = {-Math.cos(PITCH) * SQ, -Math.sin(PITCH), -Math.cos(PITCH) * SQ};
    static final double[] U = {-Math.sin(PITCH) * SQ, Math.cos(PITCH), -Math.sin(PITCH) * SQ};
    static final double[] L = norm(v(0.267, 0.802, 0.535));
    static final int DISC = 16;

    interface Tex { int get(double s, double t); }

    record Quad(double[] o, double[] eu, double[] ev, double[] n, Tex tex, boolean recv) {}

    record Disc(double[] o, double[] a, double[] b, double[] n, double t, boolean[][] op) {
        boolean contains(double[] p) {
            double[] d = sub(p, o);
            double pn = dot(d, n);
            if (Math.abs(pn) > t / 2) return false;
            int i = (int) Math.floor(dot(d, a)), j = (int) Math.floor(dot(d, b));
            return i >= 0 && j >= 0 && i < DISC && j < DISC && op[i][j];
        }
    }

    static List<Quad> quads = new ArrayList<>();
    static List<Disc> discs = new ArrayList<>();
    static double k, cx, cy;
    static int W;
    static float[] depth;
    static int[] color;

    public static void main(String[] args) throws Exception {
        String texDir = args[0];
        int size = Integer.parseInt(args[1]);
        String out = args[2];
        int bg = Integer.parseInt(System.getProperty("bg", "8B8B8B"), 16);

        BufferedImage top = ImageIO.read(new File(texDir, "jukebox_top.png"));
        BufferedImage side = ImageIO.read(new File(texDir, "jukebox_side.png"));
        BufferedImage sprA = ImageIO.read(new File(texDir, System.getProperty("discA", "music_disc_13") + ".png"));
        BufferedImage sprB = ImageIO.read(new File(texDir, System.getProperty("discB", "music_disc_creator") + ".png"));

        Tex topT = (s, t) -> top.getRGB(px(s, 16), px(t, 16));
        Tex sideT = (s, t) -> side.getRGB(px(s, 16), px(t, 16));
        add(v(0, 16, 0), v(16, 0, 0), v(0, 0, 16), topT);
        add(v(0, 16, 16), v(16, 0, 0), v(0, -16, 0), sideT);
        add(v(16, 16, 16), v(0, 0, -16), v(0, -16, 0), sideT);

        double thick = p("thick", 1);
        double[] up = v(0, 1, 0);

        double[] aA = v(1, 0, 0), bA = v(0, -1, 0);
        bA = rot(bA, aA, rad("leanA", -30));
        aA = rot(aA, up, rad("yawA", 0)); bA = rot(bA, up, rad("yawA", 0));
        disc(sprA, v(p("xA", 9), 0, 0), aA, bA, p("thickA", 2), 2, 16, hex("rimA", "404040"));

        double[] aB = v(0, 0, -1), bB = v(1, 0, 0);
        aB = rot(aB, up, rad("yawB", 0)); bB = rot(bB, up, rad("yawB", 0));
        disc(sprB, v(p("xB", 22.5), 0, p("zB", 6.5)), aB, bB, p("thickB", 2), -1, 0, hex("rimB", "-1"));

        W = size * SS;
        double[] c = v(8, 8, 8);
        k = 1;
        cx = W / 2.0 - dot(c, R) * k;
        cy = W / 2.0 + dot(c, U) * k;
        render();
        int bx0 = W, by0 = W, bx1 = -1, by1 = -1;
        for (int y = 0; y < W; y++)
            for (int x = 0; x < W; x++)
                if (color[y * W + x] != 0) {
                    bx0 = Math.min(bx0, x); bx1 = Math.max(bx1, x);
                    by0 = Math.min(by0, y); by1 = Math.max(by1, y);
                }
        double half = W / 2.0;
        double ext = Math.max(Math.max(half - bx0, bx1 + 1 - half), Math.max(half - by0, by1 + 1 - half)) / k;
        k = p("fill", 0.44) * W / ext;
        cx = W / 2.0 - dot(c, R) * k;
        cy = W / 2.0 + dot(c, U) * k;
        render();

        double[] shadow = shadow(size);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        double sh = p("shadow", 0.45);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                double f = 1 - sh * shadow[y * size + x];
                double br = ((bg >> 16) & 255) * f, bgc = ((bg >> 8) & 255) * f, bb = (bg & 255) * f;
                double r = 0, g = 0, b = 0;
                for (int j = 0; j < SS; j++)
                    for (int i = 0; i < SS; i++) {
                        int col = color[(y * SS + j) * W + x * SS + i];
                        if (col == 0) { r += br; g += bgc; b += bb; continue; }
                        r += (col >> 16) & 255; g += (col >> 8) & 255; b += col & 255;
                    }
                int n = SS * SS;
                img.setRGB(x, y, (clamp(r / n) << 16) | (clamp(g / n) << 8) | clamp(b / n));
            }
        ImageIO.write(img, "png", new File(out));
    }

    static final class Solid implements Tex {
        final int c;
        Solid(int c) { this.c = c; }
        public int get(double s, double t) { return c; }
    }

    static double[] shadow(int size) {
        double[] occ = new double[size * size];
        double sA = Math.sin(PITCH) * SQ;
        double[] lv = v(0, 1, 0);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                double X = ((x + 0.5) * SS - cx) / k, Y = ((y + 0.5) * SS - cy) / k;
                double sum = X / SQ, tot = Y / sA;
                double gx = (sum + tot) / 2, gz = (tot - sum) / 2;
                double[] g0 = v(gx, 0.01, gz);
                occ[y * size + x] = Math.max(march(g0, L) ? 1 : 0, march(g0, lv) ? 1 : 0);
            }
        double sigma = size * p("blur", 0.018);
        return blur(occ, size, sigma);
    }

    static boolean march(double[] g, double[] dir) {
        for (double s = 0; s < 30; s += 0.2) {
            double[] q = v(g[0] + dir[0] * s, g[1] + dir[1] * s, g[2] + dir[2] * s);
            if (q[0] >= 0 && q[0] <= 16 && q[1] >= 0 && q[1] <= 16 && q[2] >= 0 && q[2] <= 16) return true;
            for (Disc d : discs) if (d.contains(q)) return true;
        }
        return false;
    }

    static double[] blur(double[] src, int n, double sigma) {
        int rad = (int) Math.ceil(sigma * 3);
        double[] kern = new double[rad * 2 + 1];
        double sum = 0;
        for (int i = -rad; i <= rad; i++) { kern[i + rad] = Math.exp(-i * i / (2 * sigma * sigma)); sum += kern[i + rad]; }
        for (int i = 0; i < kern.length; i++) kern[i] /= sum;
        double[] tmp = new double[n * n], dst = new double[n * n];
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double a = 0;
                for (int i = -rad; i <= rad; i++) { int xx = Math.min(n - 1, Math.max(0, x + i)); a += src[y * n + xx] * kern[i + rad]; }
                tmp[y * n + x] = a;
            }
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double a = 0;
                for (int i = -rad; i <= rad; i++) { int yy = Math.min(n - 1, Math.max(0, y + i)); a += tmp[yy * n + x] * kern[i + rad]; }
                dst[y * n + x] = a;
            }
        return dst;
    }

    static int[][] spriteTexture(BufferedImage spr) {
        int[][] tex = new int[DISC][DISC];
        for (int j = 0; j < DISC; j++)
            for (int i = 0; i < DISC; i++) tex[i][j] = spr.getRGB(i, j);
        return tex;
    }

    static void disc(BufferedImage spr, double[] base, double[] a, double[] b, double t, int wallAxis, double wall, int rim) {
        int[][] tex = spriteTexture(spr);
        double[] n = cross(b, a);
        boolean[][] op = new boolean[DISC][DISC];
        for (int j = 0; j < DISC; j++)
            for (int i = 0; i < DISC; i++) op[i][j] = (tex[i][j] >>> 24) > 0;
        double h = DISC / 2.0;
        double minY = Double.POSITIVE_INFINITY, minWall = Double.POSITIVE_INFINITY;
        for (int j = 0; j < DISC; j++)
            for (int i = 0; i < DISC; i++) {
                if (!op[i][j]) continue;
                for (int di = 0; di <= 1; di++)
                    for (int dj = 0; dj <= 1; dj++)
                        for (int dn = -1; dn <= 1; dn += 2) {
                            double[] q = add3(scale(a, i + di - h), scale(b, j + dj - h), scale(n, dn * t / 2));
                            minY = Math.min(minY, q[1]);
                            if (wallAxis >= 0) minWall = Math.min(minWall, q[wallAxis]);
                        }
            }
        double[] c = v(base[0], base[1] - minY, base[2]);
        if (wallAxis >= 0) c[wallAxis] = wall - minWall;
        double[] o = add3(c, scale(a, -h), scale(b, -h));
        discs.add(new Disc(o, a, b, n, t, op));
        Tex face = (s, u) -> tex[px(s, DISC)][px(u, DISC)];
        add(add3(o, scale(n, t / 2), v(0, 0, 0)), scale(a, DISC), scale(b, DISC), face, n);
        add(add3(o, scale(n, -t / 2), v(0, 0, 0)), scale(a, DISC), scale(b, DISC), face, scale(n, -1));
        double[] tn = scale(n, t);
        for (int j = 0; j < DISC; j++)
            for (int i = 0; i < DISC; i++) {
                if (!op[i][j]) continue;
                double[] back = add3(o, scale(a, i), add3(scale(b, j), scale(n, -t / 2), v(0, 0, 0)));
                if (i == 0 || !op[i - 1][j]) add(back, b, tn, new Solid(rim == 0 ? tex[i][j] : rim), scale(a, -1));
                if (i == DISC - 1 || !op[i + 1][j]) add(add3(back, a, v(0, 0, 0)), b, tn, new Solid(rim == 0 ? tex[i][j] : rim), a);
                if (j == 0 || !op[i][j - 1]) add(back, a, tn, new Solid(rim == 0 ? tex[i][j] : rim), scale(b, -1));
                if (j == DISC - 1 || !op[i][j + 1]) add(add3(back, b, v(0, 0, 0)), a, tn, new Solid(rim == 0 ? tex[i][j] : rim), b);
            }
    }

    static void render() {
        depth = new float[W * W];
        color = new int[W * W];
        java.util.Arrays.fill(depth, Float.POSITIVE_INFINITY);
        for (Quad q : quads) raster(q);
    }

    static void add(double[] o, double[] eu, double[] ev, Tex tex) { quads.add(new Quad(o, eu, ev, norm(cross(ev, eu)), tex, true)); }

    static void add(double[] o, double[] eu, double[] ev, Tex tex, double[] n) { quads.add(new Quad(o, eu, ev, norm(n), tex, false)); }

    static double[][] lightDirs() {
        double[] t1 = norm(cross(L, v(0, 1, 0))), t2 = cross(L, t1);
        double spread = p("soft", 0.09);
        double[][] dirs = new double[16][];
        for (int i = 0; i < 16; i++) {
            double r = spread * Math.sqrt((i + 0.5) / 16), ang = i * 2.39996;
            dirs[i] = norm(add3(L, scale(t1, r * Math.cos(ang)), scale(t2, r * Math.sin(ang))));
        }
        return dirs;
    }

    static final double[][] LDIRS = lightDirs();

    static double blocked(double[] x) {
        int hit = 0;
        for (double[] dir : LDIRS)
            for (Disc d : discs) {
                double den = dot(dir, d.n);
                if (Math.abs(den) < 1e-6) continue;
                double s = dot(sub(d.o, x), d.n) / den;
                if (s <= 0.05) continue;
                double[] h = sub(add3(x, scale(dir, s), v(0, 0, 0)), d.o);
                int i = (int) Math.floor(dot(h, d.a)), j = (int) Math.floor(dot(h, d.b));
                if (i >= 0 && j >= 0 && i < DISC && j < DISC && d.op[i][j]) { hit++; break; }
            }
        return hit / (double) LDIRS.length;
    }

    static void raster(Quad q) {
        if (dot(q.n, F) >= -1e-9) return;
        double base = Math.min(1, 0.4 + 0.748 * Math.max(0, dot(q.n, L)));
        double wallShadow = p("wallShadow", 0.5);
        double ox = dot(q.o, R) * k + cx, oy = -dot(q.o, U) * k + cy, od = dot(q.o, F);
        double ux = dot(q.eu, R) * k, uy = -dot(q.eu, U) * k, ud = dot(q.eu, F);
        double vx = dot(q.ev, R) * k, vy = -dot(q.ev, U) * k, vd = dot(q.ev, F);
        double det = ux * vy - uy * vx;
        if (Math.abs(det) < 1e-9) return;
        int x0 = (int) Math.floor(Math.min(Math.min(ox, ox + ux), Math.min(ox + vx, ox + ux + vx)));
        int x1 = (int) Math.ceil(Math.max(Math.max(ox, ox + ux), Math.max(ox + vx, ox + ux + vx)));
        int y0 = (int) Math.floor(Math.min(Math.min(oy, oy + uy), Math.min(oy + vy, oy + uy + vy)));
        int y1 = (int) Math.ceil(Math.max(Math.max(oy, oy + uy), Math.max(oy + vy, oy + uy + vy)));
        x0 = Math.max(0, x0); y0 = Math.max(0, y0); x1 = Math.min(W - 1, x1); y1 = Math.min(W - 1, y1);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double dx = x + 0.5 - ox, dy = y + 0.5 - oy;
                double s = (dx * vy - dy * vx) / det, t = (ux * dy - uy * dx) / det;
                if (s < 0 || s >= 1 || t < 0 || t >= 1) continue;
                double d = od + s * ud + t * vd;
                int idx = y * W + x;
                if (d >= depth[idx]) continue;
                int c = q.tex.get(s, t);
                if ((c >>> 24) == 0) continue;
                depth[idx] = (float) d;
                double shade = base;
                if (q.recv) shade *= 1 - wallShadow * blocked(add3(q.o, scale(q.eu, s), scale(q.ev, t)));
                color[idx] = 0xFF000000 | (clamp(((c >> 16) & 255) * shade) << 16)
                        | (clamp(((c >> 8) & 255) * shade) << 8) | clamp((c & 255) * shade);
            }
    }

    static double p(String name, double def) { return Double.parseDouble(System.getProperty(name, String.valueOf(def))); }
    static int hex(String name, String def) { String s = System.getProperty(name, def); return s.startsWith("-") ? 0 : 0xFF000000 | Integer.parseInt(s, 16); }
    static double rad(String name, double def) { return Math.toRadians(p(name, def)); }
    static int px(double s, int n) { return Math.min(n - 1, Math.max(0, (int) Math.floor(s * n))); }
    static int clamp(double c) { return (int) Math.max(0, Math.min(255, Math.round(c))); }
    static double[] v(double x, double y, double z) { return new double[]{x, y, z}; }
    static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    static double[] sub(double[] a, double[] b) { return v(a[0] - b[0], a[1] - b[1], a[2] - b[2]); }
    static double[] scale(double[] a, double s) { return v(a[0] * s, a[1] * s, a[2] * s); }
    static double[] add3(double[] a, double[] b, double[] c) { return v(a[0] + b[0] + c[0], a[1] + b[1] + c[1], a[2] + b[2] + c[2]); }
    static double[] cross(double[] a, double[] b) { return v(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]); }
    static double[] norm(double[] a) { return scale(a, 1 / Math.sqrt(dot(a, a))); }
    static double[] rot(double[] p, double[] ax, double th) {
        double c = Math.cos(th), s = Math.sin(th);
        double[] cr = cross(ax, p);
        double d = dot(ax, p) * (1 - c);
        return v(p[0] * c + cr[0] * s + ax[0] * d, p[1] * c + cr[1] * s + ax[1] * d, p[2] * c + cr[2] * s + ax[2] * d);
    }
}
