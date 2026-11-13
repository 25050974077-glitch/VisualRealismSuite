package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

/**
 * 3D Visual Realism Suite (Minggu 11) - memperluas proyek ThreeDViewing.
 *
 * Kontrol:
 *   B : Phong <-> Blinn-Phong        F : Flat <-> Smooth shading
 *   Z : Z-Buffer (depth test) ON/OFF C : Back-Face Culling ON/OFF
 *   T : Tekstur ON/OFF               N : Filtering Nearest <-> Bilinear+Mipmap
 *   H : Bayangan ON/OFF              SPACE : cahaya berputar ON/OFF
 *   V : VSync ON/OFF (untuk ukur FPS)
 *   Mouse drag kiri / panah : orbit kamera     Scroll : zoom     ESC : keluar
 */
public class VisualRealismSuite {

    private long window;
    private int width = 1000;
    private int height = 700;

    // Kelas dari Minggu 10
    private Camera3D camera;
    private PerspectiveProjection perspective;
    private float radius = 13.0f, azimuth = 15.0f, elevation = 28.0f;
    private final float fov = 45.0f;

    // Kelas Minggu 11
    private ShaderProgram phongShader, depthShader;
    private ShadowMapFBO shadowMap;
    private Texture2D objectTexture, groundTexture;
    private RenderMesh[] shapes;      // 0 = bola, 1 = rumah
    private RenderMesh ground;
    private int shapeIndex = 0;
    private static final String[] SHAPE_NAMES = {"Sphere", "House"};

    // State toggle
    private boolean blinnPhong = true, smooth = true;
    private boolean depthTest = true, backFaceCulling = true;
    private boolean useTexture = true, mipmapFilter = true;
    private boolean shadowsOn = true, lightOrbit = false, vsync = true;
    private boolean showHouse = true;
    private float lightAngle = 0.8f;

    private boolean dragging = false;
    private double lastX, lastY;
    private double lastTime, fpsTimer;
    private int frames;
    private float fps;

    public static void main(String[] args) {
        new VisualRealismSuite().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(width, height, "Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);
        glClearColor(0.08f, 0.08f, 0.10f, 1.0f);

        int[] fbw = new int[1], fbh = new int[1];
        glfwGetFramebufferSize(window, fbw, fbh);
        width = Math.max(1, fbw[0]);
        height = Math.max(1, fbh[0]);
        glViewport(0, 0, width, height);

        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowMap = new ShadowMapFBO();
        objectTexture = new Texture2D("textures/bricks.png");
        groundTexture = new Texture2D("textures/ground.png");

        shapes = new RenderMesh[] {
            RenderMesh.createSphere(1.2f, 12, 18),
            RenderMesh.createHouse()
        };
        ground = RenderMesh.createPlane(6.0f, 4.0f);

        camera = new Camera3D(new Vector3f(0, 0, radius), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        applyCamera();
        rebuildPerspective();

        registerCallbacks();
        lastTime = glfwGetTime();
        fpsTimer = lastTime;
        updateTitle();
    }

    private void rebuildPerspective() {
        // zNear tidak terlalu dekat ke 0 -> presisi depth buffer terjaga (hindari Z-fighting)
        perspective = new PerspectiveProjection(fov, (float) width / height, 0.5f, 100.0f);
    }

    private void registerCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w <= 0 || h <= 0) return;
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
            radius = clamp(radius - (float) yoff * 0.6f, 4.0f, 30.0f);
            applyCamera();
        });
    }

    // ----------------------------------------------------------------- input
    private void onKey(int key, int action) {
        if (action == GLFW_RELEASE) return;
        boolean press = (action == GLFW_PRESS);

        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_LEFT   -> orbit(-3.0f, 0.0f);
            case GLFW_KEY_RIGHT  -> orbit(3.0f, 0.0f);
            case GLFW_KEY_UP     -> orbit(0.0f, 3.0f);
            case GLFW_KEY_DOWN   -> orbit(0.0f, -3.0f);
            case GLFW_KEY_B -> { if (press) blinnPhong = !blinnPhong; }
            case GLFW_KEY_F -> { if (press) smooth = !smooth; }
            case GLFW_KEY_Z -> { if (press) depthTest = !depthTest; }
            case GLFW_KEY_C -> { if (press) backFaceCulling = !backFaceCulling; }
            case GLFW_KEY_T -> { if (press) useTexture = !useTexture; }
            case GLFW_KEY_N -> {
                if (press) {
                    mipmapFilter = !mipmapFilter;
                    objectTexture.setFiltering(mipmapFilter);
                    groundTexture.setFiltering(mipmapFilter);
                }
            }
            case GLFW_KEY_H -> { if (press) shadowsOn = !shadowsOn; }
            case GLFW_KEY_M -> { if (press) shapeIndex = (shapeIndex + 1) % shapes.length; }
            case GLFW_KEY_SPACE -> { if (press) lightOrbit = !lightOrbit; }
            case GLFW_KEY_V -> {
                if (press) {
                    vsync = !vsync;
                    glfwSwapInterval(vsync ? 1 : 0);
                }
            }
            default -> { return; }
        }
        updateTitle();
    }

    private void orbit(float dAzimuth, float dElevation) {
        azimuth = (azimuth + dAzimuth) % 360.0f;
        elevation = clamp(elevation + dElevation, -89.0f, 89.0f);
        applyCamera();
    }

    private void applyCamera() {
        camera.updateOrbitalPosition(radius, azimuth, elevation);
    }

    // Camera3D tidak punya getter eye, jadi dihitung ulang dengan rumus yang sama
    private Vector3f computeEye() {
        float az = (float) Math.toRadians(azimuth);
        float el = (float) Math.toRadians(elevation);
        return new Vector3f(
            radius * (float) (Math.cos(el) * Math.sin(az)),
            radius * (float) Math.sin(el),
            radius * (float) (Math.cos(el) * Math.cos(az)));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private void updateTitle() {
        glfwSetWindowTitle(window, String.format(
            "Visual Realism | FPS %.0f | %s | %s | Z-Buffer %s | Cull %s | Tex %s (%s) | Shadow %s | VSync %s",
            fps,
            blinnPhong ? "Blinn-Phong" : "Phong",
            smooth ? "Smooth" : "Flat",
            onOff(depthTest), onOff(backFaceCulling),
            onOff(useTexture), mipmapFilter ? "Bilinear+Mipmap" : "Nearest",
            onOff(shadowsOn), onOff(vsync)));
    }

    // ---------------------------------------------------------------- loop
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;
            if (lightOrbit) lightAngle += dt * 0.8f;

            render();
            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            if (now - fpsTimer >= 0.5) {
                fps = (float) (frames / (now - fpsTimer));
                frames = 0;
                fpsTimer = now;
                updateTitle();
            }
        }
    }

    // -------------------------------------------------------------- render
    private void render() {
        Vector3f lightPos = new Vector3f(
            7.0f * (float) Math.cos(lightAngle), 8.0f, 7.0f * (float) Math.sin(lightAngle));

        // Matriks ruang cahaya (Ortografik untuk shadow map)
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightProj = new Matrix4f().ortho(-10.0f, 10.0f, -10.0f, 10.0f, 1.0f, 30.0f);
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ===== PASS 1: Depth Pass (dari sudut pandang cahaya) =====
        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);
        shadowMap.bindForWriting();
        HSRManager.setupDepthTest(true);          // paksa aktif selama pass bayangan
        HSRManager.setupBackFaceCulling(false);
        drawObjects(depthShader, false);
        shadowMap.unbind(width, height);

        // ===== PASS 2: Lighting Pass (dari sudut pandang kamera) =====
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCulling);

        phongShader.use();
        phongShader.setMat4("view", camera.calculateLookAtMatrix());
        phongShader.setMat4("projection", perspective.calculateProjectionMatrix());
        phongShader.setMat4("lightSpaceMatrix", lightSpace);
        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("viewPos", computeEye());
        phongShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);
        phongShader.setBool("useTexture", useTexture);
        phongShader.setBool("useBlinnPhong", blinnPhong);
        phongShader.setBool("useShadow", shadowsOn);
        phongShader.setInt("textureSampler", 0);
        phongShader.setInt("shadowMap", 1);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthTextureID());
        glActiveTexture(GL_TEXTURE0);

        drawObjects(phongShader, true);
    }

/** Menggambar lantai dan satu objek aktif, masing-masing dengan teksturnya sendiri. */
private void drawObjects(ShaderProgram s, boolean withMaterial) {
    // Lantai: tekstur lantai
    s.setMat4("model", new Matrix4f());
    if (withMaterial) {
        s.setVec3("objectColor", 0.55f, 0.55f, 0.60f);
        groundTexture.bind(0);
    }
    ground.draw(smooth);

    // Objek tunggal: tekstur objek, alas menempel di lantai
    Matrix4f model = new Matrix4f();
    if (shapeIndex == 0) {
        model.translate(0.0f, 1.2f, 0.0f);                // bola, jari-jari 1.2
    } else {
        model.translate(0.0f, 1.2f, 0.0f).scale(1.2f);    // rumah, alas di y = -1
    }
    float[][] colors = {
        {0.95f, 0.50f, 0.20f},   // bola
        {0.85f, 0.35f, 0.25f}    // rumah
    };
    s.setMat4("model", model);
    if (withMaterial) {
        s.setVec3("objectColor", colors[shapeIndex][0], colors[shapeIndex][1], colors[shapeIndex][2]);
        objectTexture.bind(0);
    }
    shapes[shapeIndex].draw(smooth);
}

    private void cleanup() {
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}