package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.grafkom.viewing3d.ProjectionTaxonomyManager.ProjectionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

/**
 * 3D Viewing Interactive Visualizer (Main Class).
 *
 * Memakai kelas-kelas modul: Camera3D, PerspectiveProjection, StereoAnaglyphRenderer,
 * ProjectionTaxonomyManager, dan Mesh3D.
 *
 * Kontrol:
 *   Mouse drag kiri / tombol panah : orbit kamera     Scroll : zoom
 *   1..6 : Orthographic, Isometric, Oblique Cavalier, Perspektif 1 / 2 / 3 titik hilang
 *   A    : anaglyph stereo ON/OFF (hanya mode perspektif)
 *   - =  : kurangi / tambah FOV (mode perspektif)
 *   H    : tampilkan rumah      T : tukar model (kubus <-> rumah)
 *   R    : reset                ESC : keluar
 */
public class ThreeDViewing {

    private long window;
    private int width = 1000;
    private int height = 700;

    // Kelas modul
    private Camera3D camera;
    private PerspectiveProjection perspective;
    private final StereoAnaglyphRenderer stereoRenderer = new StereoAnaglyphRenderer();
    private final Mesh3D cube = Mesh3D.createColorCube();
    private final Mesh3D house = Mesh3D.createHouse();

    // Camera3D & PerspectiveProjection (versi modul) tidak punya getter, jadi state dicatat di sini
    private float radius = 8.0f;
    private float azimuth = 0.0f;
    private float elevation = 0.0f;
    private float fov = 45.0f;
    private float orthoScale = 1.0f;      // zoom untuk mode paralel (volume pandang glOrtho tetap)

    private ProjectionType currentType = ProjectionType.PERSPECTIVE_3POINT;
    private boolean stereoEnabled = false;
    private boolean showHouse = false;

    private boolean dragging = false;
    private double lastX, lastY;

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal inisialisasi GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_SAMPLES, 4);
        window = glfwCreateWindow(width, height, "ThreeDViewing", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window GLFW");
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.08f, 0.08f, 0.10f, 1.0f);

        int[] fbw = new int[1], fbh = new int[1];
        glfwGetFramebufferSize(window, fbw, fbh);
        width = Math.max(1, fbw[0]);
        height = Math.max(1, fbh[0]);
        glViewport(0, 0, width, height);

        camera = new Camera3D(new Vector3f(0, 0, radius), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        rebuildPerspective();
        selectProjection(ProjectionType.PERSPECTIVE_3POINT);

        registerCallbacks();
        glfwShowWindow(window);
    }

    /** PerspectiveProjection tidak punya setter aspek, jadi dibuat ulang saat FOV / ukuran jendela berubah. */
    private void rebuildPerspective() {
        perspective = new PerspectiveProjection(fov, (float) width / height, 0.1f, 50.0f);
    }

    private void registerCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w <= 0 || h <= 0) return;      // jendela di-minimize
            width = w;
            height = h;
            glViewport(0, 0, w, h);
            rebuildPerspective();
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> onKey(key, action));

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1], y = new double[1];
                    glfwGetCursorPos(window, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (!dragging) return;
            orbit(-(float) (x - lastX) * 0.4f, (float) (y - lastY) * 0.4f);
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            if (isPerspective(currentType)) {
                radius = clamp(radius - (float) yoff * 0.5f, 3.0f, 30.0f);
                applyCamera();
            } else {
                orthoScale = clamp(orthoScale * (float) Math.pow(1.1, yoff), 0.3f, 4.0f);
            }
        });
    }

    // ----------------------------------------------------------------- input
    private void onKey(int key, int action) {
        if (action == GLFW_RELEASE) return;
        boolean press = (action == GLFW_PRESS);

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_6) {
            if (press) selectProjection(ProjectionType.values()[key - GLFW_KEY_1]);
            return;
        }

        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_LEFT   -> orbit(-3.0f, 0.0f);
            case GLFW_KEY_RIGHT  -> orbit(3.0f, 0.0f);
            case GLFW_KEY_UP     -> orbit(0.0f, 3.0f);
            case GLFW_KEY_DOWN   -> orbit(0.0f, -3.0f);
            case GLFW_KEY_MINUS  -> changeFov(-2.0f);
            case GLFW_KEY_EQUAL  -> changeFov(2.0f);
            case GLFW_KEY_A -> { if (press) stereoEnabled = !stereoEnabled; }
            case GLFW_KEY_H -> { if (press) showHouse = true; }
            case GLFW_KEY_T -> { if (press) showHouse = !showHouse; }
            case GLFW_KEY_R -> {
                if (press) {
                    radius = 8.0f;
                    fov = 45.0f;
                    orthoScale = 1.0f;
                    rebuildPerspective();
                    selectProjection(currentType);
                }
            }
            default -> { return; }
        }
        updateTitle();
    }

    private void selectProjection(ProjectionType type) {
        currentType = type;
        azimuth = presetAzimuth(type);
        elevation = presetElevation(type);
        if (!isPerspective(type)) {
            radius = clamp(radius, 4.0f, 14.0f);   // glOrtho: near = 1, far = 20
        }
        applyCamera();
        updateTitle();
    }

    // Perspektif 1/2/3 titik hilang memakai matriks proyeksi yang sama;
    // yang membedakan hanya arah pandang kamera terhadap kubus.
    private static float presetAzimuth(ProjectionType t) {
        return (t == ProjectionType.PERSPECTIVE_2POINT || t == ProjectionType.PERSPECTIVE_3POINT) ? 35.0f : 0.0f;
    }

    private static float presetElevation(ProjectionType t) {
        return (t == ProjectionType.PERSPECTIVE_3POINT) ? 30.0f : 0.0f;
    }

    private static boolean isPerspective(ProjectionType t) {
        return t == ProjectionType.PERSPECTIVE_1POINT
            || t == ProjectionType.PERSPECTIVE_2POINT
            || t == ProjectionType.PERSPECTIVE_3POINT;
    }

    private void orbit(float dAzimuth, float dElevation) {
        azimuth = (azimuth + dAzimuth) % 360.0f;
        elevation = clamp(elevation + dElevation, -89.0f, 89.0f);   // hindari gimbal flip
        applyCamera();
    }

    private void changeFov(float delta) {
        if (!isPerspective(currentType)) return;
        fov = clamp(fov + delta, 10.0f, 120.0f);
        perspective.setFovDegree(fov);
    }

    private void applyCamera() {
        camera.updateOrbitalPosition(radius, azimuth, elevation);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void updateTitle() {
        String stereo = !stereoEnabled ? "OFF"
                : (isPerspective(currentType) ? "ON" : "ON (hanya mode perspektif)");
        glfwSetWindowTitle(window, String.format("ThreeDViewing | [%d] %s | FOV %.0f | Stereo %s",
                currentType.ordinal() + 1, currentType.name(), fov, stereo));
    }

    // ---------------------------------------------------------------- render
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            render();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void render() {
        final ProjectionType type = currentType;
        float aspect = (float) width / height;

        // 1) Matriks proyeksi
        if (isPerspective(type)) {
            // Pakai PerspectiveProjection (matriks dihitung eksplisit)
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(perspective.calculateProjectionMatrix().get(new float[16]));
        } else {
            ProjectionTaxonomyManager.applyProjection(type, aspect);
            if (type == ProjectionType.OBLIQUE_CAVALIER) {
                // Shearing oblique 45 derajat (skala 1 = Cavalier): x' = x - cos(a)*z, y' = y - sin(a)*z
                float a = (float) Math.toRadians(45.0);
                Matrix4f shear = new Matrix4f().m20(-(float) Math.cos(a)).m21(-(float) Math.sin(a));
                glMultMatrixf(shear.get(new float[16]));
            }
        }

        // 2) Matriks view dari Camera3D (LookAt manual)
        Matrix4f view = camera.calculateLookAtMatrix();
        if (type == ProjectionType.ISOMETRIC) {
            view = isometricCentering().mul(view);
        } else if (type == ProjectionType.OBLIQUE_CAVALIER) {
            view = obliqueCentering().mul(view);
        }
        final Matrix4f modelView = view;

        // 3) Gambar
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);   // bersihkan R, G, B sekaligus

        if (stereoEnabled && isPerspective(type)) {
            // renderAnaglyph me-reset modelview (glLoadIdentity + translate mata), jadi view dikalikan di callback
            stereoRenderer.renderAnaglyph(() -> {
                glMultMatrixf(modelView.get(new float[16]));
                drawScene(false);
            });
        } else {
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(modelView.get(new float[16]));
            drawScene(!isPerspective(type));
        }
    }

    /**
     * ProjectionTaxonomyManager menaruh rotasi isometrik di matriks proyeksi, yang berputar
     * terhadap posisi MATA kamera (bukan terhadap kubus). Tanpa koreksi, kubus bergeser keluar layar.
     * Translasi ini memposisikan pusat kubus agar tepat berada di tengah layar setelah rotasi.
     */
    private Matrix4f isometricCentering() {
        Vector3f centerNow = new Vector3f(0, 0, -radius);
        Vector3f centerNeeded = new Vector3f(0, 0, -radius);
        new Matrix4f().rotateY((float) Math.toRadians(-45.0))
                      .rotateX((float) Math.toRadians(-35.264))
                      .transformPosition(centerNeeded);          // R^-1 * (0, 0, -radius)
        centerNeeded.sub(centerNow);
        return new Matrix4f().translation(centerNeeded);
    }

    /**
     * Shear oblique menggeser titik sebanding dengan kedalamannya (x' = x - cos(a)*z). Pusat kubus
     * berada di kedalaman -radius, jadi tanpa koreksi ia terlempar ke kanan-atas. Translasi ini
     * membatalkan pergeseran itu agar pusat kubus tetap di tengah layar.
     */
    private Matrix4f obliqueCentering() {
        float a = (float) Math.toRadians(45.0);
        return new Matrix4f().translation(-(float) Math.cos(a) * radius, -(float) Math.sin(a) * radius, 0.0f);
    }

    /** Dipanggil 1x (mode biasa) atau 2x (anaglyph: mata kiri & kanan). */
    private void drawScene(boolean scaleForParallel) {
        glPushMatrix();
        if (scaleForParallel) {
            glScalef(orthoScale, orthoScale, orthoScale);        // zoom mode paralel
        }
        drawFloorGrid();
        boolean gray = stereoEnabled && isPerspective(currentType);   // kurangi retinal rivalry
        (showHouse ? house : cube).draw(gray);
        glPopMatrix();
    }

    /** Grid lantai: memperlihatkan garis sejajar yang menyatu di titik hilang. */
    private void drawFloorGrid() {
        glLineWidth(1.0f);
        glColor3f(0.45f, 0.45f, 0.45f);
        glBegin(GL_LINES);
        for (int i = -5; i <= 5; i++) {
            glVertex3f(i, -1.0f, -5.0f);
            glVertex3f(i, -1.0f, 5.0f);
            glVertex3f(-5.0f, -1.0f, i);
            glVertex3f(5.0f, -1.0f, i);
        }
        glEnd();
    }

    private void cleanup() {
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
