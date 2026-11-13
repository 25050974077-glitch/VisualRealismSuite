package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Model 3D (Color Cube / Rumah 3D) yang didefinisikan sebagai daftar vertex + face.
 */
public class Mesh3D {

    private final float[][] vertices;   // [i] = {x, y, z}
    private final int[][] faces;        // [f] = indeks vertex (poligon cembung)
    private final float[][] faceColors; // [f] = {r, g, b}

    public Mesh3D(float[][] vertices, int[][] faces, float[][] faceColors) {
        this.vertices = vertices;
        this.faces = faces;
        this.faceColors = faceColors;
    }

    /** Kubus 2x2x2 berpusat di origin, tiap sisi berwarna berbeda. */
    public static Mesh3D createColorCube() {
        float[][] v = {
            {-1, -1,  1}, { 1, -1,  1}, { 1,  1,  1}, {-1,  1,  1},   // 0-3 depan (z=+1)
            {-1, -1, -1}, { 1, -1, -1}, { 1,  1, -1}, {-1,  1, -1}    // 4-7 belakang (z=-1)
        };
        int[][] f = {
            {0, 1, 2, 3},   // depan
            {5, 4, 7, 6},   // belakang
            {1, 5, 6, 2},   // kanan
            {4, 0, 3, 7},   // kiri
            {3, 2, 6, 7},   // atas
            {4, 5, 1, 0}    // bawah
        };
        float[][] c = {
            {1.0f, 0.15f, 0.15f},  // merah
            {0.15f, 0.9f, 0.9f},   // cyan
            {0.2f, 0.85f, 0.2f},   // hijau
            {0.9f, 0.2f, 0.9f},    // magenta
            {0.25f, 0.35f, 1.0f},  // biru
            {1.0f, 0.9f, 0.1f}     // kuning
        };
        return new Mesh3D(v, f, c);
    }

    /** Rumah sederhana: badan balok + atap pelana. */
    public static Mesh3D createHouse() {
        float[][] v = {
            {-1, -1,  1}, { 1, -1,  1}, { 1, 0.2f,  1}, {-1, 0.2f,  1},  // 0-3 dinding depan
            {-1, -1, -1}, { 1, -1, -1}, { 1, 0.2f, -1}, {-1, 0.2f, -1},  // 4-7 dinding belakang
            { 0, 1.2f,  1}, { 0, 1.2f, -1}                                // 8-9 puncak atap
        };
        int[][] f = {
            {0, 1, 2, 8, 3},   // depan + gable
            {5, 4, 7, 9, 6},   // belakang + gable
            {1, 5, 6, 2},      // dinding kanan
            {4, 0, 3, 7},      // dinding kiri
            {2, 6, 9, 8},      // atap kanan
            {7, 3, 8, 9},      // atap kiri
            {4, 5, 1, 0}       // lantai
        };
        float[][] c = {
            {0.95f, 0.85f, 0.6f}, {0.95f, 0.85f, 0.6f},
            {0.85f, 0.75f, 0.5f}, {0.85f, 0.75f, 0.5f},
            {0.75f, 0.2f, 0.15f}, {0.65f, 0.15f, 0.1f},
            {0.4f, 0.3f, 0.2f}
        };
        return new Mesh3D(v, f, c);
    }

    /** @param grayscale true = warna diubah ke luminance (mengurangi retinal rivalry pada anaglyph) */
    public void draw(boolean grayscale) {
        // Isi permukaan (didorong sedikit ke belakang supaya garis tepi tidak z-fighting)
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        for (int i = 0; i < faces.length; i++) {
            float[] c = faceColors[i];
            if (grayscale) {
                float g = 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
                glColor3f(g, g, g);
            } else {
                glColor3f(c[0], c[1], c[2]);
            }
            glBegin(GL_POLYGON);
            for (int idx : faces[i]) {
                float[] p = vertices[idx];
                glVertex3f(p[0], p[1], p[2]);
            }
            glEnd();
        }
        glDisable(GL_POLYGON_OFFSET_FILL);

        // Garis tepi
        glLineWidth(2.0f);
        glColor3f(0.0f, 0.0f, 0.0f);
        for (int[] face : faces) {
            glBegin(GL_LINE_LOOP);
            for (int idx : face) {
                float[] p = vertices[idx];
                glVertex3f(p[0], p[1], p[2]);
            }
            glEnd();
        }
    }
}
