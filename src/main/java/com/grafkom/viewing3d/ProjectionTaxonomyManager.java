package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class ProjectionTaxonomyManager {
    public enum ProjectionType {
        ORTHOGRAPHIC, ISOMETRIC, OBLIQUE_CAVALIER, 
        PERSPECTIVE_1POINT, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT
    }

    public static void applyProjection(ProjectionType type, float aspect) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        switch (type) {
            case ORTHOGRAPHIC:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, 1.0, 20.0);
                break;
            case ISOMETRIC:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, 1.0, 20.0);
                glRotatef(35.264f, 1.0f, 0.0f, 0.0f);
                glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
                break;
            case OBLIQUE_CAVALIER:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, 1.0, 20.0);
                // Terapkan matriks shearing Z miring 45 derajat
                break;
            case PERSPECTIVE_1POINT:
                gluPerspective(45.0f, aspect, 0.1f, 50.0f);
                break;
        }
    }

    // TAMBAHAN: LWJGL 3 tidak punya GLU, jadi gluPerspective dibuat sendiri memakai glFrustum.
    private static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        double top = zNear * Math.tan(Math.toRadians(fovy) / 2.0);
        double right = top * aspect;
        glFrustum(-right, right, -top, top, zNear, zFar);
    }
}
