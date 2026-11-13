package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.joml.Vector3f;

/** Mesh segitiga: interleaved [pos(3) normal(3) uv(2)], tersedia versi Flat dan Smooth. */
public class RenderMesh {
    private final int vaoSmooth, vaoFlat;
    private final int vertexCount;

    public RenderMesh(float[] positions, float[] uvs, int[] indices) {
        float[] smoothData = build(positions, uvs, indices, true);
        float[] flatData = build(positions, uvs, indices, false);
        vertexCount = smoothData.length / 8;
        vaoSmooth = upload(smoothData);
        vaoFlat = upload(flatData);
    }

    // ------------------------------------------------------------ builder
    private static String key(float x, float y, float z) {
        return Math.round(x * 10000f) + "_" + Math.round(y * 10000f) + "_" + Math.round(z * 10000f);
    }

    private static float[] build(float[] pos, float[] uv, int[] idx, boolean smooth) {
        // 1) Jumlahkan normal face (area-weighted) pada setiap posisi vertex
        Map<String, Vector3f> sums = new HashMap<>();
        for (int t = 0; t < idx.length; t += 3) {
            Vector3f n = faceNormal(pos, idx[t], idx[t + 1], idx[t + 2]);
            for (int k = 0; k < 3; k++) {
                int v = idx[t + k];
                String kk = key(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]);
                sums.computeIfAbsent(kk, s -> new Vector3f()).add(n);
            }
        }

        // 2) Susun buffer per sudut segitiga (segitiga degenerate dilewati)
        float[] out = new float[idx.length * 8];
        int o = 0;
        for (int t = 0; t < idx.length; t += 3) {
            Vector3f fn = faceNormal(pos, idx[t], idx[t + 1], idx[t + 2]);
            if (fn.lengthSquared() < 1e-12f) continue;
            Vector3f flatN = new Vector3f(fn).normalize();
            for (int k = 0; k < 3; k++) {
                int v = idx[t + k];
                Vector3f n = flatN;
                if (smooth) {
                    Vector3f s = sums.get(key(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
                    if (s != null && s.lengthSquared() > 1e-12f) n = new Vector3f(s).normalize();
                }
                out[o++] = pos[v * 3];     out[o++] = pos[v * 3 + 1]; out[o++] = pos[v * 3 + 2];
                out[o++] = n.x;            out[o++] = n.y;            out[o++] = n.z;
                out[o++] = uv[v * 2];      out[o++] = uv[v * 2 + 1];
            }
        }
        return Arrays.copyOf(out, o);
    }

    private static Vector3f faceNormal(float[] p, int a, int b, int c) {
        Vector3f pa = new Vector3f(p[a * 3], p[a * 3 + 1], p[a * 3 + 2]);
        Vector3f pb = new Vector3f(p[b * 3], p[b * 3 + 1], p[b * 3 + 2]);
        Vector3f pc = new Vector3f(p[c * 3], p[c * 3 + 1], p[c * 3 + 2]);
        return pb.sub(pa).cross(pc.sub(pa));    // tidak dinormalisasi -> berbobot luas
    }

    private static int upload(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
        return vao;
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    // ----------------------------------------------------------- bentuk
    /** Bola UV (low-poly sengaja, supaya beda Flat vs Smooth terlihat jelas). */
    public static RenderMesh createSphere(float r, int stacks, int sectors) {
        int vc = (stacks + 1) * (sectors + 1);
        float[] pos = new float[vc * 3];
        float[] uv = new float[vc * 2];
        int p = 0, t = 0;
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= sectors; j++) {
                double theta = 2.0 * Math.PI * j / sectors;
                pos[p++] = (float) (r * Math.sin(phi) * Math.cos(theta));
                pos[p++] = (float) (r * Math.cos(phi));
                pos[p++] = (float) (r * Math.sin(phi) * Math.sin(theta));
                uv[t++] = (float) j / sectors;
                uv[t++] = 1.0f - (float) i / stacks;
            }
        }
        int[] idx = new int[stacks * sectors * 6];
        int k = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int k1 = i * (sectors + 1) + j;
                int k2 = k1 + sectors + 1;
                idx[k++] = k1;     idx[k++] = k1 + 1; idx[k++] = k2;       // CCW dilihat dari luar
                idx[k++] = k1 + 1; idx[k++] = k2 + 1; idx[k++] = k2;
            }
        }
        return new RenderMesh(pos, uv, idx);
    }

    /** Kubus 2x2x2 (24 vertex, UV 0..1 per sisi). */
    public static RenderMesh createCube() {
        float[][] faces = {
            {-1, -1,  1,   1, -1,  1,   1,  1,  1,  -1,  1,  1},   // depan  (+z)
            { 1, -1, -1,  -1, -1, -1,  -1,  1, -1,   1,  1, -1},   // belakang (-z)
            { 1, -1,  1,   1, -1, -1,   1,  1, -1,   1,  1,  1},   // kanan  (+x)
            {-1, -1, -1,  -1, -1,  1,  -1,  1,  1,  -1,  1, -1},   // kiri   (-x)
            {-1,  1,  1,   1,  1,  1,   1,  1, -1,  -1,  1, -1},   // atas   (+y)
            {-1, -1, -1,   1, -1, -1,   1, -1,  1,  -1, -1,  1}    // bawah  (-y)
        };
        float[] quadUV = {0, 0, 1, 0, 1, 1, 0, 1};
        float[] pos = new float[24 * 3];
        float[] uv = new float[24 * 2];
        int[] idx = new int[36];
        for (int f = 0; f < 6; f++) {
            for (int v = 0; v < 4; v++) {
                System.arraycopy(faces[f], v * 3, pos, (f * 4 + v) * 3, 3);
                System.arraycopy(quadUV, v * 2, uv, (f * 4 + v) * 2, 2);
            }
            int b = f * 4, i = f * 6;
            idx[i] = b;     idx[i + 1] = b + 1; idx[i + 2] = b + 2;
            idx[i + 3] = b; idx[i + 4] = b + 2; idx[i + 5] = b + 3;
        }
        return new RenderMesh(pos, uv, idx);
    }

    /** Bidang lantai di y = 0 (menghadap ke atas), UV diulang 'repeat' kali. */
    public static RenderMesh createPlane(float half, float repeat) {
        float[] pos = {-half, 0, half,  half, 0, half,  half, 0, -half,  -half, 0, -half};
        float[] uv = {0, 0,  repeat, 0,  repeat, repeat,  0, repeat};
        int[] idx = {0, 1, 2,  0, 2, 3};
        return new RenderMesh(pos, uv, idx);
    }
    /** Membuat mesh dari poligon cembung (urutan titik CCW dilihat dari luar). UV dihitung dari bidang tiap face. */
public static RenderMesh createFromPolygons(float[][][] polys) {
    int vc = 0, tc = 0;
    for (float[][] poly : polys) {
        vc += poly.length;
        tc += poly.length - 2;
    }
    float[] pos = new float[vc * 3];
    float[] uv = new float[vc * 2];
    int[] idx = new int[tc * 3];
    int vi = 0, ii = 0;

    for (float[][] poly : polys) {
        Vector3f p0 = new Vector3f(poly[0][0], poly[0][1], poly[0][2]);
        Vector3f p1 = new Vector3f(poly[1][0], poly[1][1], poly[1][2]);
        Vector3f p2 = new Vector3f(poly[2][0], poly[2][1], poly[2][2]);

        // Basis bidang face: u = arah sisi pertama, v = n x u
        Vector3f u = new Vector3f(p1).sub(p0).normalize();
        Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0)).normalize();
        Vector3f v = new Vector3f(n).cross(u);

        int base = vi;
        for (float[] p : poly) {
            Vector3f d = new Vector3f(p[0] - p0.x, p[1] - p0.y, p[2] - p0.z);
            pos[vi * 3] = p[0];
            pos[vi * 3 + 1] = p[1];
            pos[vi * 3 + 2] = p[2];
            uv[vi * 2] = d.dot(u) * 0.5f;
            uv[vi * 2 + 1] = d.dot(v) * 0.5f;
            vi++;
        }
        // Triangulasi kipas (fan)
        for (int k = 1; k < poly.length - 1; k++) {
            idx[ii++] = base;
            idx[ii++] = base + k;
            idx[ii++] = base + k + 1;
        }
    }
    return new RenderMesh(pos, uv, idx);
}
    /** Rumah 3D: badan balok + atap pelana (sama dengan Mesh3D Minggu 10). */
    public static RenderMesh createHouse() {
        float[][][] polys = {
            {{-1, -1,  1}, { 1, -1,  1}, { 1, 0.2f,  1}, {0, 1.2f,  1}, {-1, 0.2f,  1}},   // depan + gable
            {{ 1, -1, -1}, {-1, -1, -1}, {-1, 0.2f, -1}, {0, 1.2f, -1}, { 1, 0.2f, -1}},   // belakang + gable
            {{ 1, -1,  1}, { 1, -1, -1}, { 1, 0.2f, -1}, { 1, 0.2f,  1}},                  // dinding kanan
            {{-1, -1, -1}, {-1, -1,  1}, {-1, 0.2f,  1}, {-1, 0.2f, -1}},                  // dinding kiri
            {{ 1, 0.2f,  1}, { 1, 0.2f, -1}, {0, 1.2f, -1}, {0, 1.2f,  1}},                // atap kanan
            {{-1, 0.2f, -1}, {-1, 0.2f,  1}, {0, 1.2f,  1}, {0, 1.2f, -1}},                // atap kiri
            {{-1, -1, -1}, { 1, -1, -1}, { 1, -1,  1}, {-1, -1,  1}}                       // lantai
        };
        return createFromPolygons(polys);
    }
}